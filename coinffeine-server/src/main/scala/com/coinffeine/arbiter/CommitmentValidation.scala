package com.coinffeine.arbiter

import com.google.bitcoin.core.Transaction

import com.coinffeine.common.PeerConnection

/** Arbiter of a handshake able to validate commitment transactions before its publication */
trait CommitmentValidation {

  /** Check the validity of a commitment transaction.
    *
    * This method ensures that the right amount is sent to the right addresses with multisig
    * and that it is well formed.
    *
    * @param committer              Who is committing resources with `commitmentTransaction`
    * @param commitmentTransaction  Transaction supposedly committing resources
    * @return                       Whether or not the commitment is valid
    */
  def isValidCommitment(committer: PeerConnection, commitmentTransaction: Transaction): Boolean
}

object CommitmentValidation {
  trait Component {
    def commitmentValidation: CommitmentValidation
  }
}
