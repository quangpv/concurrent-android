package ps.billyphan.concurrent

class UserException(id: Int) : RuntimeException("User not found for ID $id")