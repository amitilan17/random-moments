package com.example.randommemories.mainFlow

import androidx.lifecycle.ViewModel

class WriteViewModel : ViewModel() {
    val randomStringsFemale = arrayOf(
        "עכשיו אני מרגישה",
        "מה שאני רואה מולי עכשיו זה",
        "עכשיו אני חושבת על",
        "מה שמעסיק אותי כרגע זה",
        "אני נמצאת ב"
    )
    val randomStringsMale = arrayOf(
        "עכשיו אני מרגיש",
        "מה שאני רואה מולי עכשיו זה",
        "עכשיו אני חושב על",
        "מה שמעסיק אותי כרגע זה",
        "אני נמצא ב"
    )

    var userTypedText: String? = null
}