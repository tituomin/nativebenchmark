
from templating import put

t_loop = """
<% declare_idx %>
<% declare_variables %>
idx = current_size;
while (idx != 0) {
    idx--;
    <% body %>
} 
"""


t_read = put(
    t_loop,
    body = """
    <% variable_in %> = <% array_variable %>[idx];
    if (<% variable_in %> == <% element_literal %>) {
        persistentValue = 1;
    }
    else {
        persistentValue = 0;
    } """)

t_write = put(
    t_loop,
    body = """
    <% array_variable %>[idx] = <% element_literal %>; """)

