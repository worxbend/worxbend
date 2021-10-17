properties([
  buildDiscarder(
    logRotator(
      artifactDaysToKeepStr: '1', 
      artifactNumToKeepStr: '1', 
      daysToKeepStr: '1', 
      numToKeepStr: '1',
    ),
  ),
])
pipeline {
  agent none
  stages {
    stage('Test::printMessage') {
      steps {
        echo 'Finished!!!'
      }
    }
    stage('Exit') {
      steps {
        sleep(time: 20, unit: 'SECONDS')
        timestamps() {
          sleep(time: 1000, unit: 'MILLISECONDS')
        }

        echo 'Finish'
      }
    }
  }
}
