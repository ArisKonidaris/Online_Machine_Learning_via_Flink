package INFORE.utils.parsers

trait parser[T,U] {
  def parse(input: T): U
}
