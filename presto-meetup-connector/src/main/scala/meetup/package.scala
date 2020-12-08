package object meetup {
  trait PrintlnLogging {
    def log(message: String): Unit = {
      val clazz = this.getClass.getCanonicalName
      println(s">>> [$clazz] $message")
    }
  }
}
