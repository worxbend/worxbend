package com.worxbend.felis

import distage.ModuleDef

object FelisModule {

  def felisModule = new ModuleDef {
    make[FelisConfig].from(FelisConfig("Meow"))
    make[FelisDependencies]
  }
}
