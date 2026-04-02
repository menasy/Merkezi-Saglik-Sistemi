package com.menasy.merkezisagliksistemi.data.model

import android.os.Parcel
import android.os.Parcelable

data class Medicine(
    val medicineId: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val usageDescription: String = "",
    val doctorNote: String = ""
) : Parcelable {

    private constructor(parcel: Parcel) : this(
        medicineId = parcel.readString().orEmpty(),
        medicineName = parcel.readString().orEmpty(),
        dosage = parcel.readString().orEmpty(),
        frequency = parcel.readString().orEmpty(),
        usageDescription = parcel.readString().orEmpty(),
        doctorNote = parcel.readString().orEmpty()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(medicineId)
        parcel.writeString(medicineName)
        parcel.writeString(dosage)
        parcel.writeString(frequency)
        parcel.writeString(usageDescription)
        parcel.writeString(doctorNote)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Medicine> {
        override fun createFromParcel(parcel: Parcel): Medicine = Medicine(parcel)

        override fun newArray(size: Int): Array<Medicine?> = arrayOfNulls(size)
    }
}
