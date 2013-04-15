
import string
import logging
formatter = string.Formatter()

class PartialDict(dict):
    def __missing__(self, key):
        return "<% " + key + " %>"

def escape(string):
    string = string.replace('{', '__BEG__')
    string = string.replace('}', '__END__')
    string = string.replace('<% ', '{')
    string = string.replace(' %>', '}')
    return string

def unescape(string):
    string = string.replace('{', '<% ')
    string = string.replace('}', ' %>')
    string = string.replace('__BEG__', '{')
    string = string.replace('__END__', '}')
    return string

def put(template, remove = None, **kwargs):
    try:
        template = escape(template)
        for k, v in kwargs.iteritems():
            if type(v) == str:
                kwargs[k] = escape(v)

        if remove:
            for k in remove:
                kwargs[k] = ''

        fdict = PartialDict(**kwargs)
        result = formatter.vformat(template, (), fdict)
        result = unescape(result)
        return result
    except ValueError as e:
        logging.error('error with template: ' + template)
        raise e
    return None

