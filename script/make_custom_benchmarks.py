import re
import logging
from os import path
from sys import argv
import sys

from templating import put

from templates import arrays
from templates import loop_code
from templates import c_nativemethod
from templates import java_benchmark
from templates import java_registry_init

import jni_types

# Log everything, and send it to stderr.
logging.basicConfig(level=logging.DEBUG)

MAX_ALLOC_REPETITIONS = 500

i = re.IGNORECASE
begin_re = re.compile('\s*//\s*@begin\s*', flags=i)
end_re = re.compile('\s*//\s*@end\s*', flags=i)
inits_re = re.compile('\s*//\s*@inits-end\s*', flags=i)
benchmark_re = re.compile('\s*//\s*@(\S+)\s*')

def inits_block_end(line):
    return inits_re.match(line)

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
        benchmarks[lang] = {'module': None, 'benchmarks': []}

        module_start = []
        inits = []
        read_until(f, begins_block, collect = module_start)
        read_until(f, inits_block_end, collect = inits)
        benchmarks[lang]['inits'] = ''.join(inits)
        benchmarks[lang]['module'] = ''.join(module_start)
        line = read_until(f, is_benchmark_header)

        while line != '':
            bm_props = parse_benchmark_header(line)

            bm_code  = []
            line = read_until(f,
                lambda x: ends_block(x) or is_benchmark_header(x),
                collect = bm_code)

            bm_props['code'] = ''.join(bm_code)
            benchmarks[lang]['benchmarks'].append(bm_props)

            if ends_block(line):
                break

    add_field_and_array_benchmarks(benchmarks)
    add_overhead_benchmarks(benchmarks)
    return benchmarks

OVERHEAD_STEP = 2
OVERHEAD_STEPS = 11 # incl. zero
OVERHEAD_CODE_STATEMENT = "__a = (((__a * __a * __a) / __b) + __b) / __a;\n"

def add_overhead_benchmark(benchmarks, i, prefix, alloc):
    overhead_code = []
    for j in range(0, i):
        overhead_code.append(OVERHEAD_CODE_STATEMENT)

    benchmark = {
        'code'        : ''.join(overhead_code),
        'id'          : prefix + 'Overhead' + str(i).zfill(5),
        'description' : i
        }

    if alloc:
        benchmark['alloc'] = True

    c_b = benchmark.copy()
    double_benchmark = benchmark.copy()
    # double the amount of work for java (uses optimizations unlike c)
    double_benchmark['code'] = ''.join(overhead_code + overhead_code)
    j_b = double_benchmark
    c_b['direction']  = 'cj'
    j_b['direction']  = 'jj'
    benchmarks['C']['benchmarks'].append(c_b)
    benchmarks['J']['benchmarks'].append(j_b)

def add_overhead_benchmarks(benchmarks):
    for i in range(1, OVERHEAD_STEPS * OVERHEAD_STEP, OVERHEAD_STEP):
        for prefix, alloc in [('Alloc', True), ('Normal', False)]:
            add_overhead_benchmark(benchmarks, i, prefix, alloc)
    add_overhead_benchmark(benchmarks, 200, 'Warmup', False)

def macro_call(template, _type):
    return template.format(
        _type = _type['c'],
        java_type_name = _type['java'].capitalize())

def make_id(template, _type):
    return template.format(
        _type = _type['java'].capitalize())

def add_field_and_array_benchmarks(benchmarks):
    c = benchmarks['C']['benchmarks']
    java = benchmarks['J']['benchmarks']

    for _type in jni_types.primitive_types.values() + [jni_types.object_types['O']]:
        representative = _type.get('representative', False)

        c.append({
                'id' : make_id('GetStatic{_type}Field', _type),
                'representative' : representative,
                'direction' : 'cj',
                'code' : macro_call(
                    'GET_STATIC_TYPE_FIELD({_type}, {java_type_name});',
                    _type)})

        java.append({
                'id' : make_id('GetStatic{_type}Field', _type),
                'representative': representative,
                'direction' : 'jj',
                'class_init' : 'public {} persistentValue;'.format(_type['java']),
                'method_init' : '{} localPersistentValue = {};'.format(
                    _type['java'], _type.get('java-literal') or 'objectValue'),
                'code' :
                    "localPersistentValue = mockObject.{_ctype}StaticField;".format(
                        _javatype = _type['java'],
                        _ctype = _type['c']

                    ),
                'finished' : 'persistentValue = localPersistentValue;'
                })
        # todo: separate inits from global inits
        # and make side-effect real

        c.append({
                'direction' : 'cj',
                'representative' : representative,
                'id' : make_id('SetStatic{_type}Field', _type),
                'code' : macro_call(
                    'SET_STATIC_TYPE_FIELD({_type}, {java_type_name});',
                    _type)})

        java.append({
                'id' : make_id('SetStatic{_type}Field', _type),
                'representative': representative,
                'direction' : 'jj',
                'code' :
                    "mockObject.{_ctype}StaticField = {_literal} ;".format(
                        _ctype = _type['c'],
                        _literal = _type.get('java-literal') or 'objectValue'

                    ),
                })

        c.append({
                'id' : make_id('Get{_type}Field', _type),
                'direction' : 'cj',
                'representative' : representative,
                'code' : macro_call(
                    'GET_TYPE_FIELD({_type}, {java_type_name});',
                    _type)})

        java.append({
                'id' : make_id('Get{_type}Field', _type),
                'representative': representative,
                'class_init' : 'public {} persistentValue;'.format(_type['java']),
                'method_init' : '{} localPersistentValue = {};'.format(
                    _type['java'], _type.get('java-literal') or 'objectValue'),
                'direction' : 'jj',
                'code' :
                    "localPersistentValue = mockObject.{_ctype}Field;".format(
                        _javatype = _type['java'],
                        _ctype = _type['c']

                    ),
                'finished' : 'persistentValue = localPersistentValue;'
                })

        c.append({
                'id' : make_id('Set{_type}Field', _type),
                'direction' : 'cj',
                'representative' : representative,
                'code' : macro_call(
                    'SET_TYPE_FIELD({_type}, {java_type_name});',
                    _type)})

        java.append({
                'id' : make_id('Set{_type}Field', _type),
                'representative': representative,
                'direction' : 'jj',
                'code' :
                    "mockObject.{_ctype}Field = {_literal} ;".format(
                        _ctype = _type['c'],
                        _literal = _type.get('java-literal') or 'objectValue'

                    ),
                })

    for _type in jni_types.primitive_types.values():
        representative = _type.get('representative', False)
        c.append({
                'id' : make_id('New{_type}Array', _type),
                'representative' : representative,
                'direction' : 'cj',
                'vary' : 'size',
                'alloc' : 'true',
                'code' : macro_call(
                    'NEW_PRIMITIVE_ARRAY({_type}, {java_type_name});',
                    _type)
                })

        # java

        c.append({
                'id' : make_id('Get{_type}ArrayElements', _type),
                'representative' : representative,
                'direction' : 'cj',
                'vary' : 'size',
                'code' : macro_call(
                    'GET_PRIMITIVE_ARRAY_ELEMENTS({_type}, {java_type_name});' +
                    'RELEASE_PRIMITIVE_ARRAY_ELEMENTS({_type}, {java_type_name});',
                    _type)})

        c.append({
                'vary' : 'size',
                'direction' : 'cj',
                'representative' : representative,
                'id' : make_id('Get{_type}ArrayRegion', _type),
                'code' : macro_call(
                    'GET_PRIMITIVE_ARRAY_REGION({_type}, {java_type_name});',
                    _type)})

        c.append({
                'vary' : 'size',
                'representative' : representative,
                'direction' : 'cj',
                'id' : make_id('Set{_type}ArrayRegion', _type),
                'code' : macro_call(
                    'SET_PRIMITIVE_ARRAY_REGION({_type}, {java_type_name});',
                    _type)})

        c.append({
                'vary' : 'size',
                'representative' : representative,
                'direction' : 'cj',
                'id' : make_id('Get{_type}ArrayLength', _type),
                'code' : macro_call(
                    'GET_PRIMITIVE_ARRAY_LENGTH({_type});',
                    _type)})

        c.append({
                'vary' : 'size',
                'representative' : representative,
                'direction' : 'cc',
                'id' : make_id('ReadComplete{_type}Array', _type),
                'code' : put(
                    arrays.t_read,
                    declare_idx = 'jint idx;',
                    variable_in = '%s__IN' % _type['c'],
                    array_variable = '%s_buf__IN' % _type['c'],
                    element_literal = _type['c-literal']
                    )})

        java_declarations = '{0} {0}In;\n{0} [] {0}Arr = benchmarkParameter.retrieve{1}Array();'.format(
            _type['java'], _type['java'].capitalize())


        java.append({
                'vary' : 'size',
                'direction' : 'jj',
                'representative' : representative,
                'class_init' : 'public int persistentValue;',
                'method_init' : 'int localPersistentValue = 0;',
                'id' : make_id('ReadComplete{_type}Array', _type),
                'code' : put(
                    arrays.t_read,
                    declare_idx = 'int idx;',
                    declare_variables = java_declarations,
                    variable_in = '%sIn' % _type['java'],
                    array_variable = '%sArr' % _type['java'],
                    element_literal = _type['java-literal']
                    ),
                'finished' : 'persistentValue = localPersistentValue;'
                })

        c.append({
                'vary' : 'size',
                'direction' : 'cc',
                'representative' : representative,
                'id' : make_id('WriteComplete{_type}Array', _type),
                'code' : put(
                    arrays.t_write,
                    declare_idx = 'jint idx;',
                    # todo: writing affects other tests?
                    array_variable = '%s_buf__IN' % _type['c'],
                    element_literal = _type['c-literal']
                    )})

        java.append({
                'vary' : 'size',
                'direction' : 'jj',
                'representative' : representative,
                'class_init' : 'public int persistentValue;',
                'method_init' : 'int localPersistentValue = 0;',
                'id' : make_id('WriteComplete{_type}Array', _type),
                'code' : put(
                    arrays.t_write,
                    declare_variables = java_declarations,
                    declare_idx = 'int idx;',
                    # todo: writing affects other tests?
                    array_variable = '%sArr' % _type['java'],
                    element_literal = _type['java-literal']),
                'finished' : 'persistentValue = localPersistentValue;'
                })


        # NIO variations of array/buffer reading/writing
        if _type['java'] == 'boolean':
            # Not available for booleans
            continue

        if _type['java'] == 'byte':
            uppercase_typename = ''
        else:
            uppercase_typename = _type['java'].title()

        # Read with hardcoded type method
        java.append({
            'vary': 'size',
            'direction': 'jj',
            'representative': True,
            'class_init': 'public int persistentValue;',
            'method_init': put(
                arrays.t_init_nio,
                type_declarations=java_declarations),
            'id': make_id('ReadComplete{_type}NioByteBuffer', _type),
            'code': put(
                arrays.t_read_nio,
                declare_idx = 'int idx;',
                variable_in = '%sIn' % _type['java'],
                array_variable = 'directByteBufferValue',
                type_name = uppercase_typename,
                element_literal = _type['java-literal']),
            'finished': 'persistentValue = localPersistentValue;'
        })
        # Write with hardcoded type method
        java.append({
            'vary': 'size',
            'direction': 'jj',
            'representative': True,
            'class_init': 'public int persistentValue;',
            'method_init': put(
                arrays.t_init_nio,
                type_declarations=java_declarations),
            'id': make_id('WriteComplete{_type}NioByteBuffer', _type),
            'code': put(
                arrays.t_write_nio,
                declare_variables = '',
                declare_idx = 'int idx;',
                array_variable = 'directByteBufferValue',
                type_name = uppercase_typename,
                element_literal = _type['java-literal']),
            'finished': 'persistentValue = localPersistentValue;'
        })

        declaration = java_declarations + "\n"
        if _type['java'] == 'byte':
            array_variable = 'directByteBufferValue'
        else:
            # Views are only relevent for non-byte types.
            declaration += 'java.nio.{0}Buffer bufferView = directByteBufferValue.as{0}Buffer();'.format(uppercase_typename)
            array_variable = 'bufferView'

        # Bulk read through a typecast view buffer
        java.append({
            'vary': 'size',
            'direction': 'jj',
            'representative': True,
            'class_init': 'public int persistentValue;',
            'method_init': put(
                arrays.t_init_nio,
                type_declarations=declaration),
            'id': make_id('ReadBulk{_type}NioByteBufferView', _type),
            'code': put(
                arrays.t_bulk_read,
                array_variable = array_variable,
                array_in = '%sArr' % _type['java']),
            'finished': 'persistentValue = localPersistentValue;'
        })

        # Bulk write through a typecast view buffer
        java.append({
            'vary': 'size',
            'direction': 'jj',
            'representative': True,
            'class_init': 'public int persistentValue;',
            'method_init': put(arrays.t_init_nio, type_declarations=declaration),
            'id': make_id('WriteBulk{_type}NioByteBufferView', _type),
            'code': put(
                arrays.t_bulk_write,
                array_variable = array_variable,
                array_in = '%sArr' % _type['java']),
            'finished': 'persistentValue = localPersistentValue;'
        })

        if _type['java'] == 'byte':
            continue

        # Read through a typecast view buffer
        java.append({
            'vary': 'size',
            'direction': 'jj',
            'representative': True,
            'class_init': 'public int persistentValue;',
            'method_init': put(arrays.t_init_nio, type_declarations=declaration),
            'id': make_id('ReadComplete{_type}NioByteBufferView', _type),
            'code': put(
                arrays.t_read_nio_as_view,
                declare_idx = 'int idx;',
                variable_in = '%sIn' % _type['java'],
                array_variable = 'bufferView',
                type_name = uppercase_typename,
                element_literal = _type['java-literal']),
            'finished': 'persistentValue = localPersistentValue;'
        })
        # Write through a typecast view buffer
        java.append({
            'vary': 'size',
            'direction': 'jj',
            'representative': True,
            'class_init': 'public int persistentValue;',
            'method_init': put(arrays.t_init_nio, type_declarations=declaration),
            'id': make_id('WriteComplete{_type}NioByteBufferView', _type),
            'code': put(
                arrays.t_write_nio_as_view,
                declare_idx = 'int idx;',
                array_variable = 'bufferView',
                element_literal = _type['java-literal']),
            'finished': 'persistentValue = localPersistentValue;'
        })



def write_custom_benchmarks(definition_files, c_custom_output_name, java_output_dir):
    packagename = ('fi', 'helsinki', 'cs', 'tituomin', 'nativebenchmark', 'benchmark')

    all_benchmarks = read_benchmarks(definition_files)

    out_c = open(c_custom_output_name, 'w')
    out_c.write(all_benchmarks['C']['module'])

    java_classes = {} #classname, contents

    for lang, data in all_benchmarks.iteritems():
        for benchmark in data['benchmarks']:

            direction = benchmark['direction']
            from_lang, to_lang = direction[0].upper(), direction[1].upper()
            if from_lang != lang:
                logging.error("Invalid language spec.")
                exit(1)

            classname = '{0}2{1}'.format(from_lang, to_lang) + benchmark['id']
            if 'vary' in benchmark:
                dyn_par = 'true'
            else:
                dyn_par = 'false'
            if 'alloc' in benchmark:
                # large heap 128/2 = 64 Mb, 128 el 8 byte array...
                is_allocating = 'true'
            else:
                is_allocating = 'false'

            representative = benchmark.get('representative', True)

            if representative == True:
                representative = "true"
            else:
                representative = "false"

            if from_lang == 'C':
                out_c.write(put(
                        c_nativemethod.t_run_method,
                        return_type = 'void',
                        parameters = 'jobject instance',
                        function = 'runInternal',
                        packagename = '_'.join(packagename),
                        classname = classname,
                        body = put(
                            loop_code.t_c_jni_call,
                            debug = classname,
                            benchmark_body = benchmark['code'])))


                java_classes[classname] = {
                    'filename':classname + '.java',
                    'code': (put(
                        java_benchmark.t,
                        representative = representative,
                        _id = benchmark['id'],
                        packagename = '.'.join(packagename),
                        classname  = classname,
                        description = benchmark.get('description', ''),
                        is_allocating = is_allocating, # todo: measure
                        from_language = from_lang,
                        to_language = to_lang,
                        seq_no = '-1',
                        has_dynamic_parameters = dyn_par,
                        is_nonvirtual = 'false',
                        run_method = 'public native void runInternal();',
                        ))}

            elif from_lang == 'J' and to_lang == 'J':
                java_classes[classname] = {
                    'filename': classname + '.java',
                    'code': (
                        put(
                            java_benchmark.t,
                            representative = representative,
                            _id = benchmark['id'],
                            packagename = '.'.join(packagename),
                            imports = "\n".join(
                                ['import android.content.pm.PermissionInfo;',
                                 'import java.nio.ByteBuffer;'
                                 'import java.lang.ref.WeakReference;'
                                 ]),
                            classname = classname,
                            class_fields = benchmark.get('class_init', ''),
                            description = benchmark.get('description' ''),
                            is_allocating = is_allocating,
                            from_language = from_lang,
                            to_language = to_lang,
                            is_nonvirtual = 'false',
                            seq_no = '-1',
                            has_dynamic_parameters = dyn_par,
                            run_method = put(
                                java_benchmark.java_run_method_inline_t,
                                init = data['inits'],
                                type_init = benchmark.get('method_init', ''),
                                loop = put(
                                    loop_code.t_java,
                                    finished = benchmark.get('finished', ''),
                                    benchmark_body = benchmark['code']))))}

    out_c.flush()
    out_c.close()

    for classname, contents in java_classes.iteritems():
        f = open(path.join(java_output_dir, '/'.join(packagename), contents['filename']), 'w')
        f.write(contents['code'])
        f.flush()
        f.close()

    return (['.'.join(packagename + (classname,)) for classname in java_classes.keys()])
