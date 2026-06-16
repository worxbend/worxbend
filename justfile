create-main-scala-app:
    pipx run cookiecutter .cookiecutter/scala/app --output-dir ./applications
create-scala-app GROUP_PREFIX:
    pipx run cookiecutter .cookiecutter/scala/app --output-dir ./applications/{{GROUP_PREFIX}}
create-main-scala-lib:
    pipx run cookiecutter .cookiecutter/scala/app --output-dir ./libs
create-scala-lib GROUP_PREFIX:
    pipx run cookiecutter .cookiecutter/scala/app --output-dir ./libs/{{GROUP_PREFIX}}
create-java-app:
    echo "java app"
create-java-lib:
    echo "java lib"
clean:
    rm -rf out/ .bloop .bsp
mill-clean:
    fd -H -I -p 'out' -t d -E '.git' | xargs rm -rf
metals-clean:
    fd -p -H -I '.*/\.bloop$' -t d | xargs rm -rf
    fd -p -H -I '.*/\.metals$' -t d | xargs rm -rf
test-all:
    echo "test all"
