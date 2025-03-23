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
    rm -rf out/ .bloop .bsp target/
    rm -rf $(fd -H -I 'target' -t d -E 'project')

metals-clean:
    rm -rf $(fd -H -I 'metals.sbt' -t f)
    rm -rf $(fd -H -I '.bloop' -t d)
    rm -rf $(fd -H -I '.metals' -t d)



test-all:
    echo "test all"
