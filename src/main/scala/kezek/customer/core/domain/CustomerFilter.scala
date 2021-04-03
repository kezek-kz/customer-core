package kezek.customer.core.domain

trait CustomerFilter

object CustomerFilter {

  case class ByFirstNameFilter(firstName: String) extends CustomerFilter
  case class ByLastNameFilter(lastName: String) extends CustomerFilter
  case class ByEmailFilter(email: String) extends CustomerFilter
  case class ByPhoneNumberFilter(phoneNumber: String) extends CustomerFilter

}
