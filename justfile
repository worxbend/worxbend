create-scala-app:
    echo "scala app"
create-scala-lib:
    echo "scala lib"
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