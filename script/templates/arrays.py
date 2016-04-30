from templating import partial

t_loop = """
<% declare_idx %>
<% declare_variables %>
for (idx = 0; idx < current_size; idx++) {
    <% body %>
}
"""

t_read = partial(
    t_loop,
    body = """
    <% variable_in %> = <% array_variable %>[idx];
    """)

t_write = partial(
    t_loop,
    body = """
    <% array_variable %>[idx] = <% element_literal %>; """)

t_init_nio = """
    <% type_declarations %>
    int localPersistentValue = 0;
    current_size /= 64;

"""

t_read_nio = partial(
    t_loop,
    body = """
    <% variable_in %> = <% array_variable %>.get<% type_name %>(idx);
    """)

t_write_nio = partial(
    t_loop,
    body = """
    <% array_variable %>.put<% type_name %>(idx, <% element_literal %>);
    """)

t_read_nio_as_view = partial(
    t_loop,
    body = """
    <% variable_in %> = <% array_variable %>.get(idx);
    """)

t_write_nio_as_view = partial(
    t_loop,
    body = """
    <% array_variable %>.put(idx, <% element_literal %>);
    """)

t_bulk_read = """
<% declare_variables %>
<% array_variable %>.get(<% array_in %>, 0, current_size);
"""

t_bulk_write = """
<% declare_variables %>
<% array_variable %>.put(<% array_in %>, 0, current_size);
"""
