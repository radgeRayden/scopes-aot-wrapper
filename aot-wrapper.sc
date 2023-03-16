vvv bind C
do
    let header =
        include
            """"void abort();
                int printf(const char *restrict format, ...);
    using header.extern
    unlet header
    locals;

# we redefine assert to avoid depending on the scopes runtime.
spice _aot-assert (args...)
    inline check-assertion (result anchor msg)
        if (not result)
            C.printf "%s assertion failed: %s \n"
                anchor as rawstring
                msg
            C.abort;
    let argc = ('argcount args)
    verify-count argc 2 2
    let expr msg =
        'getarg args 0
        'getarg args 1
    let msgT = ('typeof msg)
    if ((not (msgT < zarray)) and (msgT != rawstring))
        error "string expected as second argument"
    let anchor = ('anchor args)
    let anchor-text = (repr anchor)
    'tag `(check-assertion expr [anchor-text] (msg as rawstring)) anchor

sugar aot-assert (args...)
    let args = (args... as list)
    let cond msg body = (decons args 2)
    let anchor = ('anchor cond)
    let msg = (convert-assert-args args cond msg)
    list ('tag `_aot-assert anchor) cond msg

run-stage;

set-globals!
    ..
        do
            let assert = aot-assert
            locals;
        (globals)

run-stage;

let m argc argv = (script-launch-args)

if (argc < 1)
    error "expected 1 extra argument: entry-module"

entry-module := (string (argv @ 0))
main := ((require-from "." entry-module __env) as Closure)

run-stage;

compile-object
    default-target-triple
    compiler-file-kind-object
    "./build/scopes-app.o"
    do
        let main = (static-typify main i32 (mutable@ rawstring))
        locals;
