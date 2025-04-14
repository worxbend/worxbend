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
    fd -H -I -p 'target' -t d -E 'project' | xargs rm -rf
deep-clean:
    fd -H -I -p '.*/project/target/*' -t d | xargs rm -rf
    fd -H -I -p '.*/project/target/*' -t d | xargs rm -rf
metals-clean:
    fd -p -H -I '.*/metals.sbt$' -t f | xargs rm -rf
    fd -p -H -I '.*/\.bloop$' -t d | xargs rm -rf
    fd -p -H -I '.*/\.metals$' -t d | xargs rm -rf
sbt-clean:
    fd -p -H -I '.*\.sbt$' -t f | xargs rm -rf
test-all:
    echo "test all"
