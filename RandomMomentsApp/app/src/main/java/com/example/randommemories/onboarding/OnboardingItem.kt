package com.example.randommemories.onboarding

import androidx.annotation.RawRes
import androidx.annotation.StringRes

data class OnboardingItem(
    @StringRes val title: Int,
    @StringRes val text1: Int,
    @StringRes val text2: Int? = null,
    @StringRes val text3: Int? = null,
    @StringRes val text4: Int? = null,
    @StringRes val text5: Int? = null,
    @RawRes val video: Int,
    @StringRes val buttonText: Int? = null
)