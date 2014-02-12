
from benchmark_generator import generate_benchmarks, packagename
from make_custom_benchmarks import write_custom_benchmarks
from templates import java_registry_init
from templating import put

import sys
from sys import argv
import os.path
import os
import logging

# Log everything, and send it to stderr.
logging.basicConfig(level=logging.DEBUG)

def write_benchmark(benchmark, java_output_dir):
        java_output_path = os.path.join(
            java_output_dir,
            benchmark["path"])

        try:
            os.makedirs(java_output_path)
        except OSError:
            pass
        
        java_output = open(
            os.path.join(
                java_output_path,
                benchmark["filename"]), 'w')

        java_output.write(benchmark["code"])
    

def write_benchmarks(c_output, c_runners_output, java_output_dir):
    benchmarks = generate_benchmarks()

    c_output.write(benchmarks['c'])
    c_runners_output.write(benchmarks['c_runners'])

    write_benchmark(benchmarks['java_counterparts'], java_output_dir)
    for benchmark in benchmarks['java']:
        write_benchmark(benchmark, java_output_dir)

    return [benchmark['class'] for benchmark in benchmarks['java']]


def write_benchmark_initialiser(classes):
    benchmark_inits = []

    for _class in classes:
        benchmark_inits.append(java_registry_init.inits(_class))

    path = os.path.join(
        java_output_dir,
        'fi/helsinki/cs/tituomin/nativebenchmark',
        'BenchmarkInitialiser.java')

    init_output = open(path, 'w')
    init_output.write(put(java_registry_init.t, register_benchmarks = "\n".join(benchmark_inits)))
        
 
if __name__ == "__main__":
    try:
        argv.pop(0)
        c_output_name            = argv.pop(0)
        c_run_output_name        = argv.pop(0)
        c_custom_output_name     = argv.pop(0)
        java_output_dir          = argv.pop(0)
        c_definition_filename    = argv.pop(0)
        java_definition_filename = argv.pop(0)

        definition_files = {
                'C' : open(c_definition_filename),
                'J' : open(java_definition_filename)}

        c_run_output = open(c_run_output_name, 'w')
        c_output =     open(c_output_name, 'w')

        classes = (write_benchmarks(c_output, c_run_output, java_output_dir) +
                   write_custom_benchmarks(definition_files, c_custom_output_name, java_output_dir))

        write_benchmark_initialiser(classes)
        print(",".join(classes))
    except Exception as e:
        logging.exception("Exception was thrown.")
        sys.exit(1)
    else:
        sys.exit(0)
