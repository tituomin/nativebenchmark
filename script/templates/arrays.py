
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

