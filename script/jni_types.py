
import itertools

primitive_types = None
object_types = None
other_types = None
types = None
return_types = None

representative_types = None

array_types = None

primitive_type_definitions = [
    {
        'symbol'       : 'b',
        'java'         : 'boolean',
        'c'            : 'jboolean',
        'c-literal'    : '1',
        'java-literal' : 'true',
        'jvm-desc'     : 'Z',
        'byte_count'   : '1'
        },

    {
        'symbol'       : 'y',
        'java'         : 'byte',
        'c'            : 'jbyte',
        'c-literal'    : "'a'",
        'java-literal' : '(byte)100',
        'jvm-desc'     : 'B',
        'byte_count'   : '1',
        'representative' : True,
        # todo: same value?
        },

    {
        'symbol'       : 'c',
        'java'         : 'char',
        'c'            : 'jchar',
        'c-literal'    : '12',
        'java-literal' : "'\u0012'",
        'jvm-desc'     : 'C',
        'byte_count'   : '2'
        },

    {
        'symbol'       : 's',
        'java'         : 'short',
        'c'            : 'jshort',
        'c-literal'    : '101',
        'java-literal' : '(short)101',
        'jvm-desc'     : 'S',
        'byte_count'   : '2'
        },

    {
        'symbol'       : 'i',
        'java'         : 'int',
        'c'            : 'jint',
        'c-literal'    : '102',
        'java-literal' : '102',
        'jvm-desc'     : 'I',
        'representative' : True,
        'byte_count'   : '4'
        },

    {
        'symbol'       : 'l',
        'java'         : 'long',
        'c'            : 'jlong',
        'c-literal'    : '103',
        'java-literal' : '103',
        'jvm-desc'     : 'J',
        'representative' : True,
        'byte_count'   : '8'
        },

    {
        'symbol'       : 'f',
        'java'         : 'float',
        'c'            : 'jfloat',
        'c-literal'    : '104.1',
        'java-literal' : '104.1f',
        'jvm-desc'     : 'F',
        'representative' : True,
        'byte_count'   : '4'
        },

    {
        'symbol'       : 'd',
        'java'         : 'double',
        'c'            : 'jdouble',
        'c-literal'    : '105.1',
        'java-literal' : '105.1',
        'jvm-desc'     : 'D',
        'representative' : True,
        'byte_count'   : '8'
        },
]

object_type_definitions = [

    {
        'symbol'       : 'O',
        'java'         : 'Object',
        'package'      : 'java.lang',
        'c'            : 'jobject',
        'c-literal'    : None,
        'java-literal' : None,
        'is-object'    : True,
        'representative' : True,
        'jvm-desc'     : 'Ljava/lang/Object;'
        },

    {
        'symbol'       : 'C',
        'java'         : 'Class',
        'package'      : 'java.lang',
        'c'            : 'jclass',
        'c-literal'    : None,
        'java-literal' : None,
        'is-object'    : True,
        'jvm-desc'     : 'Ljava/lang/Class;'
        },

    {
        'symbol'       : 'S',
        'java'         : 'String',
        'package'      : 'java.lang',
        'c'            : 'jstring',
        'c-literal'    : None,
        'java-literal' : '"a string"',
        'is-object'    : True,
        'jvm-desc'     : 'Ljava/lang/String;'
        },

    {
        'symbol'       : 'T',
        'java'         : 'Throwable',
        'package'      : 'java.lang',
        'c'            : 'jthrowable',
        'c-literal'    : None,
        'java-literal' : None,
        'is-object'    : True,
        'jvm-desc'     : 'Ljava/lang/Throwable;'
        }
    
]

other_type_definitions = [

    {
        'symbol'       : 'v',
        'java'         : 'void',
        'c'            : 'void',
        'c-literal'    : None,
        'java-literal' : None,
        'representative' : True,        
        'jvm-desc'     : 'V'
        }
]


def java_native_methodname(is_static, returntype, parametertypes):
    ret = "_"
    if is_static:
        ret += "st_"
    ret += types.get(returntype)['java'] + "_"
    for t in parametertypes:
        ret += types.get(t)

def java_native_methodsignature(is_static, returntype, parametertypes):
    ret = "private"
    if is_static:
        ret += "static "
    
    # todo here

def type_combinations(size=0, typeset=None):
    if size == 0:
        size = len(typeset)

    return list(itertools.islice(itertools.cycle(typeset), 0, size))

def method_descriptor(return_type, parameter_types):
    return "({parameters}){returndesc}".format(
        parameters = ''.join([td['jvm-desc'] for td in parameter_types]),
        returndesc = return_type['jvm-desc']);
    

def init_types():
    global primitive_types, object_types, other_types, types, return_types, array_types, representative_types

    primitive_types = dict([(typedef['symbol'], typedef) for typedef in primitive_type_definitions])
    object_types = dict([(typedef['symbol'], typedef) for typedef in object_type_definitions])
    other_types = dict([(typedef['symbol'], typedef) for typedef in other_type_definitions])

    array_element_types = {}
    array_element_types.update(primitive_types)
    array_element_types['O'] = object_types['O']

    # todo here
    array_types = dict(
        [
            ('A' + key,
             {'symbol'            : 'A' + key,
              'java'              : tipe['java'] + '[]',
              'package'           : tipe.get('package', None),
              'c'                 : tipe['c'] + 'Array',
              'c-literal'         : None,
              'java-literal'      : None,
              'is-array'          : True,
              'representative'    : tipe.get('representative', False),
              'java-element-type' : tipe['java'],
              'c-element-type'    : tipe['c'],
              'jvm-desc'          : '[' + tipe['jvm-desc']
              })
            for key, tipe in array_element_types.iteritems()])

    types = dict()
    types.update(primitive_types)
    types.update(object_types)
    types.update(array_types)

    return_types = dict()
    return_types.update(types)
    return_types.update(other_types)

#    types.update(other_types)

init_types()

