package net.afsal.evmap.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GalleryViewModel : ViewModel() {
    val galleryPosition = MutableLiveData<Int>()
}