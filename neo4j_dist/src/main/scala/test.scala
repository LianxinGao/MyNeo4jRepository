import org.neo4j.driver.v1.{AuthTokens, Driver, GraphDatabase}

object test {
  def main(args: Array[String]): Unit = {
    val localDriver:Driver = GraphDatabase.driver(s"bolt://localhost:7687", AuthTokens.basic("neo4j", "123123"))
    val tx = localDriver.session().beginTransaction()

//    val result = tx.run("match (n) where id(n)=0 return (n)")
      val result = tx.run("create(n:Person)")
    tx.success()
    tx.close()
    println(result)
    while (result.hasNext){
      println(result.next())
    }
//    localDriver.close()
  }
}
