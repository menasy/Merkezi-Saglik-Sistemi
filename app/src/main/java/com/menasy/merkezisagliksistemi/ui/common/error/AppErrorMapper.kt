package com.menasy.merkezisagliksistemi.ui.common.error

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.menasy.merkezisagliksistemi.ui.common.message.UiMessage
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.TimeoutCancellationException

object AppErrorMapper {

    fun map(
        throwable: Throwable?,
        operationType: OperationType = OperationType.GENERAL
    ): UiMessage {
        val resolved = unwrapThrowable(throwable)

        return when (resolved) {
            is AppException -> mapAppException(resolved.reason)
            is FirebaseAuthException -> mapFirebaseAuthException(resolved, operationType)
            is FirebaseFirestoreException -> mapFirestoreException(resolved, operationType)
            is FirebaseNetworkException,
            is UnknownHostException,
            is ConnectException,
            is NoRouteToHostException -> networkMessage()

            is FirebaseTooManyRequestsException -> tooManyRequestsMessage()
            is SocketTimeoutException,
            is TimeoutCancellationException -> timeoutMessage()

            is SecurityException -> permissionDeniedMessage()
            is IOException -> networkMessage()
            else -> operationFallback(operationType)
        }
    }

    fun mapReason(reason: AppErrorReason): UiMessage {
        return mapAppException(reason)
    }

    private fun unwrapThrowable(throwable: Throwable?): Throwable? {
        if (throwable == null) return null

        var current: Throwable = throwable
        while (
            current.cause != null &&
            current !is AppException &&
            current !is FirebaseAuthException &&
            current !is FirebaseFirestoreException &&
            current !is IOException &&
            current !is TimeoutCancellationException
        ) {
            current = current.cause ?: break
        }
        return current
    }

    private fun mapAppException(reason: AppErrorReason): UiMessage {
        return when (reason) {
            AppErrorReason.EMAIL_AND_PASSWORD_REQUIRED -> UiMessage.warning(
                title = "Eksik Bilgi",
                description = "E-posta ve şifre alanlarını doldurun."
            )

            AppErrorReason.REQUIRED_FIELDS -> UiMessage.warning(
                title = "Eksik Bilgi",
                description = "Lütfen tüm zorunlu alanları doldurun."
            )

            AppErrorReason.INVALID_TC_NO -> UiMessage.warning(
                title = "Geçersiz TC Kimlik Numarası",
                description = "TC kimlik numarası 11 haneli olmalıdır."
            )

            AppErrorReason.CITY_SELECTION_REQUIRED -> UiMessage.warning(
                title = "İl Seçimi Gerekli",
                description = "Randevu araması için bir il seçin."
            )

            AppErrorReason.BRANCH_SELECTION_REQUIRED -> UiMessage.warning(
                title = "Poliklinik Seçimi Gerekli",
                description = "Randevu araması için bir poliklinik seçin."
            )

            AppErrorReason.START_DATE_REQUIRED -> UiMessage.warning(
                title = "Başlangıç Tarihi Gerekli",
                description = "Lütfen başlangıç tarihini seçin."
            )

            AppErrorReason.END_DATE_REQUIRED -> UiMessage.warning(
                title = "Bitiş Tarihi Gerekli",
                description = "Lütfen bitiş tarihini seçin."
            )

            AppErrorReason.PAST_DATE_NOT_ALLOWED -> UiMessage.warning(
                title = "Geçersiz Tarih",
                description = "Geçmiş tarihler için randevu araması yapılamaz."
            )

            AppErrorReason.END_DATE_BEFORE_START -> UiMessage.warning(
                title = "Geçersiz Tarih Aralığı",
                description = "Bitiş tarihi başlangıç tarihinden önce olamaz."
            )

            AppErrorReason.DATE_RANGE_TOO_LONG -> UiMessage.warning(
                title = "Tarih Aralığı Çok Geniş",
                description = "Tarih aralığı en fazla 15 gün olabilir."
            )

            AppErrorReason.SLOT_SELECTION_REQUIRED -> UiMessage.warning(
                title = "Saat Seçimi Gerekli",
                description = "Devam etmek için uygun bir saat seçin."
            )

            AppErrorReason.SEARCH_CRITERIA_MISSING -> UiMessage.error(
                title = "Arama Bilgisi Eksik",
                description = "Randevu arama kriterleri bulunamadı. Lütfen yeniden arama yapın."
            )

            AppErrorReason.DOCTOR_AVAILABILITY_MISSING -> UiMessage.error(
                title = "Uygunluk Bilgisi Bulunamadı",
                description = "Doktor uygunluk bilgisi yüklenemedi. Lütfen tekrar deneyin."
            )

            AppErrorReason.APPOINTMENT_CONFIRMATION_MISSING -> UiMessage.error(
                title = "Onay Bilgisi Eksik",
                description = "Randevu onay bilgisi bulunamadı."
            )

            AppErrorReason.APPOINTMENT_INFO_MISSING -> UiMessage.error(
                title = "Randevu Bilgisi Eksik",
                description = "Randevu bilgisi bulunamadı. Lütfen işlemi yeniden başlatın."
            )

            AppErrorReason.SLOT_ALREADY_TAKEN -> UiMessage.warning(
                title = "Saat Dolu",
                description = "Bu saat az önce başka bir hasta tarafından alındı. Lütfen başka bir saat seçin."
            )

            AppErrorReason.APPOINTMENT_NOT_FOUND -> UiMessage.error(
                title = "Randevu Bulunamadı",
                description = "İptal edilmek istenen randevu bulunamadı."
            )

            AppErrorReason.APPOINTMENT_CREATION_FAILED -> UiMessage.error(
                title = "Randevu Oluşturulamadı",
                description = "Randevu oluşturulurken bir hata oluştu. Lütfen tekrar deneyin."
            )

            AppErrorReason.INVALID_USER_ROLE -> UiMessage.error(
                title = "Hesap Türü Geçersiz",
                description = "Kullanıcı rolü doğrulanamadı."
            )

            AppErrorReason.USER_UID_MISSING -> UiMessage.error(
                title = "Kayıt İşlemi Başarısız",
                description = "Kullanıcı kimliği oluşturulamadı. Lütfen tekrar deneyin."
            )

            AppErrorReason.USER_NOT_FOUND -> UiMessage.error(
                title = "Kullanıcı Bulunamadı",
                description = "Bu e-posta ile kayıtlı kullanıcı bulunamadı."
            )

            AppErrorReason.USER_RECORD_NOT_FOUND -> UiMessage.error(
                title = "Hesap Bilgisi Bulunamadı",
                description = "Kullanıcı kaydı sistemde bulunamadı."
            )

            AppErrorReason.USER_ROLE_NOT_FOUND -> UiMessage.error(
                title = "Rol Bilgisi Eksik",
                description = "Kullanıcı rol bilgisi bulunamadı."
            )

            AppErrorReason.USER_FULL_NAME_NOT_FOUND -> UiMessage.warning(
                title = "Profil Bilgisi Eksik",
                description = "Kullanıcı adı bilgisi alınamadı."
            )

            AppErrorReason.NO_ACTIVE_SESSION -> UiMessage.warning(
                title = "Oturum Bulunamadı",
                description = "Devam etmek için tekrar giriş yapın."
            )
        }
    }

    private fun mapFirebaseAuthException(
        exception: FirebaseAuthException,
        operationType: OperationType
    ): UiMessage {
        val code = exception.errorCode

        return when {
            code == "ERROR_WRONG_PASSWORD" ||
                code == "ERROR_INVALID_CREDENTIAL" ||
                code == "ERROR_INVALID_LOGIN_CREDENTIALS" ||
                exception is FirebaseAuthInvalidCredentialsException -> UiMessage.error(
                title = "Giriş Bilgileri Hatalı",
                description = "Şifre hatalı."
            )

            code == "ERROR_USER_NOT_FOUND" ||
                exception is FirebaseAuthInvalidUserException -> UiMessage.error(
                title = "Kullanıcı Bulunamadı",
                description = "Bu e-posta ile kayıtlı kullanıcı bulunamadı."
            )

            code == "ERROR_EMAIL_ALREADY_IN_USE" ||
                code == "ERROR_CREDENTIAL_ALREADY_IN_USE" ||
                exception is FirebaseAuthUserCollisionException -> UiMessage.error(
                title = "E-posta Kullanımda",
                description = "Bu e-posta adresi zaten kullanımda."
            )

            code == "ERROR_WEAK_PASSWORD" ||
                exception is FirebaseAuthWeakPasswordException -> UiMessage.warning(
                title = "Zayıf Şifre",
                description = "Şifreniz çok zayıf. En az 6 karakter kullanın."
            )

            code == "ERROR_INVALID_EMAIL" -> UiMessage.warning(
                title = "Geçersiz E-posta",
                description = "E-posta adresini doğru formatta girin."
            )

            code == "ERROR_USER_DISABLED" -> UiMessage.error(
                title = "Hesap Devre Dışı",
                description = "Bu hesap devre dışı bırakılmış."
            )

            code == "ERROR_NETWORK_REQUEST_FAILED" ||
                exception is FirebaseNetworkException -> networkMessage()

            code == "ERROR_TOO_MANY_REQUESTS" ||
                exception is FirebaseTooManyRequestsException -> tooManyRequestsMessage()

            code == "ERROR_OPERATION_NOT_ALLOWED" -> UiMessage.error(
                title = "İşlem Kullanılamıyor",
                description = "Bu işlem şu anda kullanılamıyor."
            )

            code == "ERROR_REQUIRES_RECENT_LOGIN" ||
                exception is FirebaseAuthRecentLoginRequiredException -> UiMessage.warning(
                title = "Yeniden Giriş Gerekli",
                description = "Güvenlik nedeniyle tekrar giriş yapmanız gerekiyor."
            )

            code == "ERROR_USER_TOKEN_EXPIRED" -> UiMessage.warning(
                title = "Oturum Süresi Doldu",
                description = "Lütfen yeniden giriş yapın."
            )

            else -> operationFallback(operationType)
        }
    }

    private fun mapFirestoreException(
        exception: FirebaseFirestoreException,
        operationType: OperationType
    ): UiMessage {
        return when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> permissionDeniedMessage()
            FirebaseFirestoreException.Code.UNAVAILABLE -> networkMessage()
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> timeoutMessage()
            FirebaseFirestoreException.Code.NOT_FOUND -> UiMessage.error(
                title = "Kayıt Bulunamadı",
                description = "İstenen kayıt bulunamadı."
            )

            FirebaseFirestoreException.Code.ALREADY_EXISTS -> UiMessage.warning(
                title = "Kayıt Zaten Var",
                description = "Bu kayıt sistemde zaten mevcut."
            )

            FirebaseFirestoreException.Code.UNAUTHENTICATED -> UiMessage.warning(
                title = "Oturum Doğrulanamadı",
                description = "Lütfen tekrar giriş yapın."
            )

            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> UiMessage.error(
                title = "Sunucu Yoğun",
                description = "İşlem şu anda tamamlanamıyor. Lütfen biraz sonra tekrar deneyin."
            )

            FirebaseFirestoreException.Code.CANCELLED,
            FirebaseFirestoreException.Code.ABORTED -> UiMessage.warning(
                title = "İşlem Tamamlanamadı",
                description = "İşlem yarıda kesildi. Lütfen tekrar deneyin."
            )

            FirebaseFirestoreException.Code.INVALID_ARGUMENT,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION,
            FirebaseFirestoreException.Code.OUT_OF_RANGE -> UiMessage.warning(
                title = "Geçersiz İşlem",
                description = "Gönderilen bilgiler doğrulanamadı."
            )

            FirebaseFirestoreException.Code.DATA_LOSS,
            FirebaseFirestoreException.Code.INTERNAL,
            FirebaseFirestoreException.Code.UNKNOWN,
            FirebaseFirestoreException.Code.UNIMPLEMENTED,
            FirebaseFirestoreException.Code.OK -> operationFallback(operationType)
        }
    }

    private fun permissionDeniedMessage(): UiMessage {
        return UiMessage.error(
            title = "Yetki Hatası",
            description = "Bu işlemi yapmak için yetkiniz bulunmuyor."
        )
    }

    private fun networkMessage(): UiMessage {
        return UiMessage.error(
            title = "Bağlantı Sorunu",
            description = "İnternet bağlantınızı kontrol edin."
        )
    }

    private fun timeoutMessage(): UiMessage {
        return UiMessage.warning(
            title = "Zaman Aşımı",
            description = "İstek zaman aşımına uğradı. Lütfen tekrar deneyin."
        )
    }

    private fun tooManyRequestsMessage(): UiMessage {
        return UiMessage.warning(
            title = "Çok Fazla Deneme",
            description = "Kısa sürede çok fazla istek gönderildi. Lütfen biraz sonra tekrar deneyin."
        )
    }

    private fun operationFallback(operationType: OperationType): UiMessage {
        return when (operationType) {
            OperationType.LOGIN -> UiMessage.error(
                title = "Giriş Başarısız",
                description = "Giriş sırasında bir hata oluştu, lütfen tekrar deneyin."
            )

            OperationType.REGISTER -> UiMessage.error(
                title = "Kayıt Başarısız",
                description = "Kayıt işlemi sırasında bir hata oluştu, lütfen tekrar deneyin."
            )

            OperationType.FETCH_DATA -> UiMessage.error(
                title = "Veriler Alınamadı",
                description = "Veri alınırken bir hata oluştu, lütfen tekrar deneyin."
            )

            OperationType.CREATE -> UiMessage.error(
                title = "Kayıt Oluşturulamadı",
                description = "Kayıt oluşturulurken bir hata oluştu, lütfen tekrar deneyin."
            )

            OperationType.UPDATE -> UiMessage.error(
                title = "Güncelleme Başarısız",
                description = "Güncelleme sırasında bir hata oluştu, lütfen tekrar deneyin."
            )

            OperationType.DELETE -> UiMessage.error(
                title = "Silme İşlemi Başarısız",
                description = "Silme işlemi sırasında bir hata oluştu, lütfen tekrar deneyin."
            )

            OperationType.SESSION -> UiMessage.error(
                title = "Oturum Hatası",
                description = "Oturum işlemi sırasında bir hata oluştu, lütfen tekrar giriş yapın."
            )

            OperationType.VALIDATION -> UiMessage.warning(
                title = "Bilgileri Kontrol Edin",
                description = "Lütfen girdiğiniz bilgileri kontrol edip tekrar deneyin."
            )

            OperationType.APPOINTMENT -> UiMessage.error(
                title = "Randevu İşlemi Başarısız",
                description = "Randevu işlemi sırasında bir hata oluştu, lütfen tekrar deneyin."
            )

            OperationType.GENERAL -> UiMessage.error(
                title = "İşlem Başarısız",
                description = "İşlem sırasında bir hata oluştu, lütfen tekrar deneyin."
            )
        }
    }
}
