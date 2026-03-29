public void reverseTransaction(int alertId) throws Exception {
    // Get the alert
    FraudAlert alert = fraudAlertDAO.findById(alertId);
    if (alert == null) throw new Exception("Alert not found");
    if (alert.isReversed()) throw new Exception("Transaction already reversed");

    // Get the transaction
    Transaction tx = txDAO.findById(alert.getTransactionId());
    if (tx == null) throw new Exception("Transaction not found");

    // Only reverse WITHDRAWAL or TRANSFER
    if (tx.getTransactionType() == Transaction.TransactionType.DEPOSIT) {
        throw new Exception("Deposit transactions cannot be reversed");
    }

    // Credit back the amount
    Account account = accountDAO.findById(tx.getAccountId());
    account.deposit(tx.getAmount());
    accountDAO.updateBalance(account.getId(), account.getBalance());

    // Save reversal transaction
    Transaction reversal = new Transaction(
        tx.getAccountId(),
        Transaction.TransactionType.DEPOSIT,
        tx.getAmount(),
        account.getBalance(),
        "Reversal of flagged transaction #" + tx.getId()
    );
    txDAO.save(reversal);

    // Mark alert as resolved and reversed
    fraudAlertDAO.resolveAndReverse(alertId);
}
