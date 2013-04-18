import re
import logging
from os import path
from sys import argv
import sys

from templating import put
from templates import loop_code
from templates import c_nativemethod
from templates import java_benchmark
from templates import java_registry_init

import jni_types

# Log everything, and send it to stderr.
logging.basicConfig(level=logging.DEBUG)


i = re.IGNORECASE
begin_re = re.compile('\s*//\s*@begin\s*', flags=i)
end_re = re.compile('\s*//\s*@end\s*', flags=i)
benchmark_re = re.compile('\s*//\s*@(\S+)\s*')

def begins_block(line):
    return begin_re.match(line)

def ends_block(line):
    return end_re.match(line)

def is_benchmark_header(line):
    return benchmark_re.match(line)

def parse_benchmark_header(line):
    tokens = line.split()[1:]
    b_properties = parse_properties(tokens[1:])
    b_properties['id'] = tokens[0][1:]
    return b_properties

def parse_properties(seq):
    kvs = []
    for s in seq:
        splitted = s.split('=')
        kvs.append((splitted[0], splitted[1]))
    try:
        return dict(kvs)
    except ValueError as e:
        print seq
        print seq[0].split('=')
        exit(1)

def abort_if_last(line):
    if line == '':
        logging.error("Invalid benchmark input file.")
        exit(1)

def read_until(f, predicate, collect=None):
    line = ''
    while not predicate(line):
        if collect != None:
            collect.append(line)
        line = f.readline()
        abort_if_last(line)
    abort_if_last(line)
    return line

def read_benchmarks(definition_files):
    benchmarks = {}
    for lang, f in definition_files.iteritems():
        benchmarks[lang] = []

        module_start = []
        read_until(f, begins_block, collect = module_start)
        line = read_until(f, is_benchmark_header)

        while line != '':
            bm_props = parse_benchmark_header(line)
            
            bm_code  = []
            line = read_until(f,
                lambda x: ends_block(x) or is_benchmark_header(x),
                collect = bm_code)

            bm_props['code'] = ''.join(bm_code)
            benchmarks[lang].append(bm_props)

            if ends_block(line):
                break

    add_field_and_array_benchmarks(benchmarks)            
    add_overhead_benchmarks(benchmarks)
    return ''.join(module_start), benchmarks

OVERHEAD_STEP = 100
OVERHEAD_STEPS = 11 # incl. zero
OVERHEAD_CODE_STATEMENT = "__a = (((__a * __a * __a) / __b) + __b) / __a;\n"

def add_overhead_benchmarks(benchmarks):
    for i in range(0, OVERHEAD_STEPS * OVERHEAD_STEP, OVERHEAD_STEP):
        overhead_code = []
        for j in range(0, i):
            overhead_code.append(OVERHEAD_CODE_STATEMENT)

        benchmarks['C'].append({
                'code':  ''.join(overhead_code),
                'description' : i,
                'id'  : 'Overhead' + str(i).zfill(5) })


def macro_call(template, _type):
    return template.format(
        _type = _type['c'],
        java_type_name = _type['java'].capitalize())

def make_id(template, _type):
    return template.format(
        _type = _type['java'].capitalize())

def add_field_and_array_benchmarks(benchmarks):
    bmc = benchmarks['C']

    for _type in jni_types.primitive_types.values() + [jni_types.object_types['O']]:
        bmc.append({
                'id' : make_id('GetStatic{_type}Field', _type),
                'code' : macro_call(
                    'GET_STATIC_TYPE_FIELD({_type}, {java_type_name});',
                    _type)})

        bmc.append({
                'id' : make_id('SetStatic{_type}Field', _type),
                'code' : macro_call(
                    'SET_STATIC_TYPE_FIELD({_type}, {java_type_name});',
                    _type)})

        bmc.append({
                'id' : make_id('Get{_type}Field', _type),
                'code' : macro_call(
                    'GET_TYPE_FIELD({_type}, {java_type_name});',
                    _type)})

        bmc.append({
                'id' : make_id('Set{_type}Field', _type),
                'code' : macro_call(
                    'SET_TYPE_FIELD({_type}, {java_type_name});',
                    _type)})

    for _type in jni_types.primitive_types.values():
        bmc.append({
                'id' : make_id('New{_type}Array', _type),
                'vary' : 'size',
                'code' : macro_call(
                    'NEW_PRIMITIVE_ARRAY({_type}, {java_type_name});',
                    _type)
                })

        bmc.append({
                'id' : make_id('Get{_type}ArrayElements', _type),
                'vary' : 'size',
                'code' : macro_call(
                    'GET_PRIMITIVE_ARRAY_ELEMENTS({_type}, {java_type_name});' +
                    'RELEASE_PRIMITIVE_ARRAY_ELEMENTS({_type}, {java_type_name});',
                    _type)})

        bmc.append({
                'vary' : 'size',
                'id' : make_id('Get{_type}ArrayRegion', _type),
                'code' : macro_call(
                    'GET_PRIMITIVE_ARRAY_REGION({_type}, {java_type_name});',
                    _type)})
        
        bmc.append({
                'vary' : 'size',
                'id' : make_id('Set{_type}ArrayRegion', _type),
                'code' : macro_call(
                    'SET_PRIMITIVE_ARRAY_REGION({_type}, {java_type_name});',
                    _type)})

            
def write_custom_benchmarks(definition_files, java_output_dir):
    packagename = ('fi', 'helsinki', 'cs', 'tituomin', 'nativebenchmark', 'benchmark')
    file_beginning, all_benchmarks = read_benchmarks(definition_files)

    c_dir = path.dirname(definition_files['C'].name)
    out_c = open(path.join(c_dir, 'custom_benchmarks.c'), 'w')

    out_c.write(file_beginning)

    java_classes = {} #classname, contents

    for benchmark in all_benchmarks['C']:

        classname = 'C2J' + benchmark['id']
        if 'vary' in benchmark:
            dyn_par = 'true'
        else:
            dyn_par = 'false'

        out_c.write(put(
                c_nativemethod.t_run_method,
                return_type = 'void',
                parameters = 'jobject instance',
                function = 'run',
                packagename = '_'.join(packagename),
                classname = classname,
                purge = True,
                body = put(
                    loop_code.t_c_jni_call,
                    debug = classname,
                    benchmark_body = benchmark['code'])))

        java_classes[classname] = {
            'filename':classname + '.java',
            'code': (put(
                java_benchmark.t,
                packagename = '.'.join(packagename),
                classname  = classname,
                description = benchmark.get('description', ''),
                from_language = 'C',
                to_language = 'J',
                seq_no = '-1',
                has_dynamic_parameters = dyn_par,
                run_method = 'public native void run();',
                purge = True))}


    out_c.flush()
    out_c.close()

#    for benchmark in all_benchmarks['J']:
#        pass # todo

    for classname, contents in java_classes.iteritems():
        f = open(path.join(java_output_dir, '/'.join(packagename), contents['filename']), 'w')
        f.write(contents['code'])
        f.flush()
        f.close()

    return (['.'.join(packagename + (classname,)) for classname in java_classes.keys()])
