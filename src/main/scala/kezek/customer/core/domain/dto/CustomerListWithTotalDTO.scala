package kezek.customer.core.domain.dto

import kezek.customer.core.domain.Customer

case class CustomerListWithTotalDTO(total: Long, collection: Seq[Customer])
