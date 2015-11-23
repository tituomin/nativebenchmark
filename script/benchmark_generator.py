from templates import java_benchmark
from templates import java_counterparts
from templates import c_module
from templates import c_jni_function
from templates import c_nativemethod

from templating import put

import itertools
import logging

import jni_types
from jni_types import types, primitive_types, return_types

packagename = ['fi', 'helsinki', 'cs', 'tituomin', 'nativebenchmark', 'benchmark']
library_name = 'nativebenchmark'
java_counterpart_classname = 'JavaCounterparts'
native_method_name = 'nativemethod'                                        
class_counter = 0

# todo: initialize complex return values for c side

def next_sequence_no():
    global class_counter
    class_counter += 1
    return str(class_counter).zfill(5)

def benchmark_classname(prefix, number):
    return prefix + 'Benchmark' + number

def parameter_initialisation(language, typespec, name):
    if typespec.get('is-array', False):
        if language == 'java':
            expression = 'benchmarkParameter.retrieve{_type}Array()'.format(
                _type = typespec['java-element-type'].capitalize())
        else:
            expression = '{_type}ArrayValue'.format(
                _type = typespec['c-element-type'])

    elif typespec.get('is-object', False):
        if language == 'java':
            expression = 'benchmarkParameter.retrieve{_type}()'.format(
                _type = typespec['java'])
        else:
            expression = '{_type}Value'.format(
                _type = typespec['c'])

    elif language == 'java':
        if typespec.get('java-literal'):
            expression = typespec['java-literal']
        else:
            expression = ''

    elif language == 'c':
        if typespec.get('c-literal'):
            expression = typespec['c-literal']
        else:
            expression = ''

    if name:
        return name + " = " + expression
    else:
        return expression

def modifier_combinations():
    privacy = ['public', 'private', 'protected']
    static = ['static', '']
    return list(itertools.product(privacy, static))

def method_combinations():
    combinations = []
    combinations.append({
            'description'      : 'no arguments or return types',
            'representative'   : True,
            'return_types'     : [return_types['v']],
            'target_modifiers' : modifier_combinations(),
            'types'            : []})

    for symbol, _type in types.iteritems():
        combinations.append({
            'description'      : 'varying count {0}'.format(symbol),
            'representative'   : _type.get('representative', False),
            'return_types'     : [return_types['v']],
            'target_modifiers' : [('public', '')],
            'types'            : jni_types.type_combinations(
                size = 20,
                typeset = [types[symbol]])})

    # Start from 1 to avoid duplicates.
    combinations.append({
            'description'      : 'vary number of types',
            'representative'   : True,
            'skip'             : 1,
            'return_types'     : [return_types['v']],
            'target_modifiers' : [('public', '')],
            'types'            : jni_types.type_combinations(
                typeset = types.values())
            })

    combinations.append({
            'description'      : 'modifier combinations',
            'representative'   : True,
            'return_types'     : [return_types['l']],
            'target_modifiers' : modifier_combinations(),
            'include_nonvirtual' : True,
            'types'            : [types['i']]
            })

    filtered_return_types = filter(
        lambda x : x['symbol'] != 'l',
        jni_types.type_combinations(typeset = types.values()))

    combinations.append({
            'description'      : 'return types',
            'return_types'     : filtered_return_types,
            'target_modifiers' : [('public', '')],
            'types'            : [types['i']]
            })

    return combinations

def generate_benchmarks():
    global class_counter
    java = []
    java_callees = {}
    c_implementations = []
    c_runners = []
    c_methodid_inits = []

    for spec in method_combinations():
        virtualities = [True]
        if spec.get('include_nonvirtual'):
            virtualities.append(False)
        for virtualcall in virtualities:
            for target_modifier in spec['target_modifiers']:
                for return_type in spec['return_types']:

                    if 'representative' not in spec:
                        representative = return_type.get('representative', False)
                    else:
                        representative = spec['representative']

                    representative = "true" if representative else "false"

                    type_combination = spec['types']

                    native_method_modifiers = " ".join(target_modifier)
                    return_expression = parameter_initialisation('c', return_type, None)

                    # Declare/initialize the parameters that are passed to the called
                    # function/method.

                    parameter_names = []

                    parameter_declarations = []
                    parameter_initialisations = []
                    c_parameter_declarations = []
                    c_parameter_initialisations = []

                    if target_modifier[1] == 'static':
                        c_parameter_declarations.append('jclass cls')
                    else:
                        c_parameter_declarations.append('jobject instance')

                    for i, type_data in enumerate(type_combination):
                        parameter_names.append(type_data['symbol'] + str(i+1))

                        parameter_declarations.append(
                            type_data['java'] + ' ' + parameter_names[-1])
                        c_parameter_declarations.append(
                            type_data['c'] + ' ' + parameter_names[-1])
                        parameter_initialisations.append(
                            parameter_initialisation(
                                'java', type_data, parameter_names[-1]))
                        c_parameter_initialisations.append(
                            parameter_initialisation(
                                'c', type_data, parameter_names[-1]))

                    skip = spec.get('skip', 0)
                    upper_bound = len(type_combination)
                    if upper_bound == 0: upper_bound = 1
                    for i in range(skip, upper_bound):

                        sequence_no = next_sequence_no()

                        for from_lang, to_lang in [
                                ('J', 'C'), ('J', 'J'),
                                ('C', 'C'), ('C', 'J')]:

                            pairing = (from_lang, to_lang)

                            if virtualcall == False:
                                # Nonvirtual calls only apply C -> J instance methods
                                # J2C is generated as a proxy
                                # to get benchmark metadata
                                if (target_modifier[1] == 'static' or
                                    pairing in [('J', 'J'), ('C', 'C')]):
                                        continue

                            # 1. Set up call targets.

                            if to_lang == 'C':
                                counterpart_method_name = 'nativemethod'
                            if to_lang == 'J':
                                counterpart_method_unqualified = (
                                    'benchmark' + sequence_no)

                            if pairing == ('J', 'J'):
                                if target_modifier[0] == 'private':
                                    # A private method cannot
                                    # be called from Java.
                                    # Don't generate a benchmark.
                                    continue

                                if target_modifier[1] == 'static':
                                    clsname = java_counterpart_classname
                                else:
                                    clsname = 'counterpartInstance'     
                                counterpart_method_name = (
                                    clsname + '.' + counterpart_method_unqualified)
                                native_method = ''

                            if pairing == ('C', 'J'):
                                counterpart_method_name = 'benchmark' + sequence_no

                            # 2. Set up calling sources.

                            if from_lang == 'C':
                                run_method = java_benchmark.native_run_method_t
                                native_method = ''

                            if from_lang == 'J':
                                run_method = put(
                                    java_benchmark.java_run_method_t,
                                    parameter_declarations       = "; ".join(parameter_declarations[0:i+1]),
                                    parameter_initialisations    = "; ".join(parameter_initialisations[0:i+1]),
                                    counterpart_method_name      = counterpart_method_name,
                                    counterpart_method_arguments = ", ".join(parameter_names[0:i+1]))

                            if pairing == ('J', 'C'):
                                native_method = put(
                                    java_benchmark.native_method_t,
                                    modifiers                = native_method_modifiers,

                                    return_type              = return_type['java'],
                                    name                     = native_method_name,
                                    parameters               = ", ".join(parameter_declarations[0:i+1]))

                            # 3. Common benchmark class wrapper for all benchmarks.

                            classname = benchmark_classname(
                                '2'.join(pairing), sequence_no)

                            java.append({
                                    'filename' : classname + ".java",
                                    'class'    : '.'.join(packagename) + "." + classname,
                                    'path'     : '/'.join(packagename),
                                    'code'     : put(
                                        java_benchmark.t,
                                        representative               = representative,
                                        imports                      = '',
                                        has_dynamic_parameters       = 'false', # todo
                                        is_allocating                = 'false',
                                        is_nonvirtual                = 'false' if virtualcall else 'true',
                                        _id                          = benchmark_classname("", sequence_no),
                                        description                  = spec['description'],
                                        seq_no                       = class_counter,
                                        from_language                = from_lang,
                                        to_language                  = to_lang,
                                        class_relations              = '',
                                        packagename                  = '.'.join(packagename),
                                        classname                    = classname,
                                        library_name                 = library_name,
                                        run_method                   = run_method,
                                        native_method                = native_method if to_lang =='C' else '')})


                            if return_type.get('is-object') or return_type.get('is-array'):
                                ret_expression = "{_type}Value".format(
                                    _type = return_type['c'])
                            else:
                                ret_expression = parameter_initialisation('java', return_type, None)

                            # 4. Call target implementations.

                            if to_lang == 'J':
                                java_callees[counterpart_method_unqualified] = put(
                                        java_counterparts.counterpart_t,
                                        return_type       = return_type['java'],
                                        privacy           = target_modifier[0],
                                        static            = target_modifier[1],
                                        methodname        = counterpart_method_unqualified,
                                        parameters        = ", ".join(parameter_declarations[0:i+1]),
                                        return_expression = ret_expression
                                        )

                                # todo append return expressions !! (performance optimisation removala)
                                # to counterpart class (at the end or beginning) ?

                            if pairing == ('J', 'C'):
                                c_implementations.append(
                                    put(c_nativemethod.t,
                                        return_type       = return_type['c'],
                                        packagename       = ('_').join(packagename),
                                        classname         = classname,
                                        function          = native_method_name,
                                        parameters        = ", ".join(c_parameter_declarations[0:i+2]),
                                        return_expression = return_expression))

                            if pairing == ('C', 'C'):
                                jni_param_names = ['env', 'instance']
                                jni_param_names.extend(parameter_names)

                                c_runners.append(
                                    put(c_nativemethod.t_caller_native,
                                        packagename = '_'.join(packagename),
                                        classname = classname,
                                        parameter_declarations = "; ".join(c_parameter_declarations[1:i+2]),
                                        parameter_initialisations = "; ".join(c_parameter_initialisations[0:i+1]),
                                        counterpart_method_name = "Java_{package}_{cls}_{function}".format(
                                            package = ('_').join(packagename),
                                            cls = 'J2CBenchmark' + str(sequence_no),
                                            function = native_method_name),
                                        counterpart_method_arguments = ", ".join(jni_param_names[0:i+3])))

                            if pairing == ('C', 'J'):
                                if return_type.get('is-object', return_type.get('is-array', False)):
                                    java_method_type = 'Object'
                                else:
                                    java_method_type = return_type['java'].capitalize()

                                arguments = ', '.join(parameter_names[0:i+1])
                                if arguments != '':
                                    arguments = ', ' + arguments

                                c_runners.append(
                                    c_nativemethod.call_java_from_c(
                                        static = (target_modifier[1] == 'static'),
                                        nonvirtual = not virtualcall,
                                        seq_no = class_counter,
                                        packagename = '_'.join(packagename),
                                        classname = classname,
                                        parameter_declarations = "; ".join(c_parameter_declarations[1:i+2]),
                                        parameter_initialisations = "; ".join(c_parameter_initialisations[0:i+1]),
                                        java_method_type = java_method_type,
                                        call_variant= '', # todo test variants?
                                        arguments = arguments))

                                c_methodid_inits.append(
                                    put(c_module.mid_init_t,
                                        seq_no = class_counter,
                                        static = target_modifier[1].capitalize(),
                                        method_name = counterpart_method_name,
                                        method_descriptor = jni_types.method_descriptor(
                                            return_type, type_combination[0:i+1])))

    ref_types = jni_types.object_types.values() + jni_types.array_types.values()
    jcp_decl = ''
    jcp_init = ''
    for _type in ref_types:
        jcp_decl += "private static {_type1} {_type2}Value;\n".format(
            _type1 = _type['java'], _type2 = _type['c'])
        jcp_init += parameter_initialisation(
            'java', _type, _type['c'] + 'Value') + ";\n"

    
    java_counterparts_class = {
            'filename' : java_counterpart_classname + ".java",
            'class'    : '.'.join(packagename) + "." + java_counterpart_classname,
            'path'     : '/'.join(packagename),
            'code'     : put(java_counterparts.t,
                packagename = '.'.join(packagename),
                imports = '',
                return_value_declarations = jcp_decl,
                return_value_inits = jcp_init,
                             counterpart_methods = ''.join(java_callees.values()))}

    c_file = put(
        c_module.t,
        jni_function_templates = ''.join(c_implementations),
        initialisers='')
    
    c_runners_file = put(
        c_module.t,
        jni_function_templates = ''.join(c_runners),
        initialisers= put(
            c_module.initialisers_t,
            mid_inits = ''.join(c_methodid_inits),
            amount_of_methods = len(c_methodid_inits)))

    return { 'java'              : java,
             'java_counterparts' : java_counterparts_class,
             'c'                 : c_file,
             'c_runners'         : c_runners_file }
