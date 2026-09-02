package com.homiq.app.domain

import com.homiq.app.data.model.DepositStatus

object DepositRules {
    fun remainingSen(
        depositAmountSen: Long,
        returnedAmountSen: Long,
    ): Long =
        (depositAmountSen - returnedAmountSen).coerceAtLeast(0L)

    fun statusAfterReturn(
        depositAmountSen: Long,
        returnedAmountSen: Long,
    ): DepositStatus =
        if (returnedAmountSen >= depositAmountSen) {
            DepositStatus.RETURNED
        } else {
            DepositStatus.PARTIALLY_RETURNED
        }
}
