package ooo.simone.vibescout.core.exeptions

class NoNetworkException(message : String? = "No network") : Exception(message)
class NoAuthKeyException(message : String? = "No auth key") : Exception(message)
class NoPermissionsException(message : String? = "No permission granted") : Exception(message)
class ReleaseWifiLockException(message : String? = "Unable to release wifi lock") : Exception(message)
