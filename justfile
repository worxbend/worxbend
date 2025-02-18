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
    rm -rf out/ .bloop target/
    rm -rf $(fd -H -I 'target' -t d -E 'project')
    rm -rf $(fd -H -I -t d 'project' | rg 'project/project')
    rm -rf $(fd -H -I -t d 'project' | rg 'project/target')

test-all:
    echo "test all"
