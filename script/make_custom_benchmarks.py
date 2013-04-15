import re
import logging
from os import path

from templates import c_nativemethod
from templates import java_benchmark

# Log everything, and send it to stderr.
logging.basicConfig(level=logging.DEBUG)


begin_re = re.compile('\s*//\s*@begin\s*', flags=re.IGNORECASE)
end_re = re.compile('\s*//\s*@end\s*', flags=re.IGNORECASE)
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
    return dict([(sp.split('=') for sp in seq)])

def abort_if_last(line):
    if line == '':
        logging.error("Invalid benchmark input file.")
        exit(1)

def read_until(f, predicate, line, collect=None):
    line = ''
    while not predicate(line):
        if collect != None:
            collect += line
        line = f.readline()
    abort_if_last(line)
    return line

def read_benchmarks(definition_files):
    benchmarks = {}
    for lang, f in definition_files:
        benchmarks[lang] = []

        read_until(f, begins_block)
        line = read_until(f, is_benchmark_header)

        while True:
            bm_props = parse_benchmark_header(line)
            
            bm_code  = ''
            read_until(f,
                lambda x: ends_block(x) or is_benchmark_header(x),
                collect = bm_code)

            bm_props['code'] = bm_code

            if ends_block(line):
                benchmarks[lang].append(bm_props)

    return benchmarks
            
def generate_manual_benchmarks(definitions_files, java_output_dir):
    all_benchmarks = read_benchmarks(definition_files)

    c_dir = path.dirname(definition_files['C'])
    out_c = open(path.join(c_dir, 'custom_benchmarks.c'), 'w')

    java_classes = {} #classname, contents
    packagename = ('fi', 'helsinki', 'cs', 'tituomin', 'nativebenchmark', 'benchmark')

    for benchmark in all_benchmarks['C']:

        out_c.write(c_nativemethod.t_caller_native.format(
            packagename = '_'.join(packagename),
            classname = benchmark['id'],
            parameter_declarations = ''
            parameter_initialisations = ''
            counterpart_method_name = benchmark['code'],
            counterpart_method_arguments = '')) # todo hack dont work - need to modify template

        java_classes[benchmark['id']] = (
            java_benchmark.t.format(
                packagename = '.'.join(packagename),
                imports = '',
                classname  = benchmark['id'],
                from_language = 'C',
                to_language = 'J',
                seq_no = '-1',
                description = '',
                native_method = '',
                run_method = java_benchmark.native_method_t))

    out_c.flush()
    out_c.close()

    for benchmark in all_benchmarks['J']:
        pass # todo

    for classname, contents in java_classes.iteritems():
        f = open(path.join(java_output_dir, classname + '.java'), 'w')
        f.write(contents)
        f.flush()
        f.close()

if __name__ == "__main__":
    try:
        definition_files = {
            'C' : open(argv[1])
            'J' : open(argv[2])
            }

        java_output_dir = argv[3]
        generate_manual_benchmarks(definition_files, java_output_dir)

    except Exception as e:
        logging.exception("Exception was thrown.")
        sys.exit(1)
    else:
        sys.exit(0)
