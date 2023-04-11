package io.kzonix.reqflect.services

trait ServerInfoProviderService {

  def getNetforkInterfaces(): String
  def getCpuDetais(): String
  def getMemoryDetais(): String



}


