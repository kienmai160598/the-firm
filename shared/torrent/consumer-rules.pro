# libmovies_engine.so binds its JNI functions by name (Java_com_nuvio_torrent_MoviesEngineJni_*),
# so R8 in the app must keep the class and its native methods as they are.
-keep class com.nuvio.torrent.MoviesEngineJni {
    native <methods>;
}
