from templating import partial

t_loop = """
<% declare_idx %>
<% declare_variables %>
idx = current_size;
while (idx != 0) {
    idx--;
    <% body %>
} 
"""

t_read = partial(
    t_loop,
    body = """
    <% variable_in %> = <% array_variable %>[idx];
    if (<% variable_in %> == <% element_literal %>) {
        localPersistentValue = 1;
    }
    else {
        localPersistentValue = 0;
    } """)

t_write = partial(
    t_loop,
    body = """
    <% array_variable %>[idx] = <% element_literal %>; """)

t_read_nio = partial(
    t_loop,
    body = """
    directByteBufferValue.clear();
    <% array_variable %>.clear();
    <% variable_in %> = <% array_variable %>.get<% type_name %>();
    if (<% variable_in %> == <% element_literal %>) {
        localPersistentValue = 1;
    }
    else {
        localPersistentValue = 0;
    } """)

t_write_nio = partial(
    t_loop,
    body = """
    directByteBufferValue.clear();
    <% array_variable %>.clear();
    <% array_variable %>.put<% type_name %>(<% element_literal %>);
    """)

t_read_nio_as_view = partial(
    t_loop,
    body = """
    directByteBufferValue.clear();
    <% array_variable %>.clear();
    <% variable_in %> = <% array_variable %>.get();
    if (<% variable_in %> == <% element_literal %>) {
        localPersistentValue = 1;
    }
    else {
        localPersistentValue = 0;
    } """)

t_write_nio_as_view = partial(
    t_loop,
    body = """
    <% array_variable %>.clear();
    <% array_variable %>.put(<% element_literal %>);
    """)

t_bulk_read = """
<% declare_variables %>
directByteBufferValue.clear();
<% array_variable %>.clear();
<% array_variable %>.get(<% array_in %>, 0, current_size);
"""

t_bulk_write = """
<% declare_variables %>
directByteBufferValue.clear();
<% array_variable %>.clear();
<% array_variable %>.put(<% array_in %>, 0, current_size);
"""
