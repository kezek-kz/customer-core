package kezek.customer.core.repository

import akka.Done
import kezek.customer.core.domain.{Customer, CustomerFilter}
import kezek.customer.core.util.SortType

import scala.concurrent.Future

trait CustomerRepository {

  def create(customer: Customer): Future[Customer]

  def update(id: String, customer: Customer): Future[Customer]

  def findById(id: String): Future[Option[Customer]]

  def paginate(filters: Seq[CustomerFilter],
               page: Option[Int],
               pageSize: Option[Int],
               sortParams: Map[String, SortType]): Future[Seq[Customer]]

  def count(filters: Seq[CustomerFilter]): Future[Long]

  def delete(id: String): Future[Done]
}
