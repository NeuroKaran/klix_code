#define _GNU_SOURCE
#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <poll.h>
#include <pty.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

static void free_string_array(char **arr) {
    if (!arr) return;
    for (size_t i = 0; arr[i] != NULL; i++) {
        free(arr[i]);
    }
    free(arr);
}

static char **copy_jstring_array(JNIEnv *env, jobjectArray jarr) {
    if (!jarr) {
        char **empty = (char **)calloc(1, sizeof(char *));
        return empty;
    }
    jsize len = (*env)->GetArrayLength(env, jarr);
    char **out = (char **)calloc((size_t)len + 1, sizeof(char *));
    if (!out) return NULL;

    for (jsize i = 0; i < len; i++) {
        jstring jstr = (jstring)(*env)->GetObjectArrayElement(env, jarr, i);
        if (!jstr) {
            out[i] = strdup("");
        } else {
            const char *utf = (*env)->GetStringUTFChars(env, jstr, NULL);
            out[i] = utf ? strdup(utf) : strdup("");
            if (utf) (*env)->ReleaseStringUTFChars(env, jstr, utf);
            (*env)->DeleteLocalRef(env, jstr);
        }
        if (!out[i]) {
            free_string_array(out);
            return NULL;
        }
    }
    out[len] = NULL;
    return out;
}

static void init_default_termios(struct termios *tio) {
    memset(tio, 0, sizeof(*tio));
    tio->c_iflag = ICRNL | IXON;
#ifdef IUTF8
    tio->c_iflag |= IUTF8;
#endif
    tio->c_oflag = OPOST | ONLCR;
    tio->c_cflag = CS8 | CREAD | HUPCL;
    tio->c_lflag = ICANON | ISIG | ECHO | ECHOE | ECHOK | ECHOCTL | ECHOKE | IEXTEN;
    tio->c_cc[VINTR] = 3;     /* Ctrl-C */
    tio->c_cc[VQUIT] = 28;    /* Ctrl-\ */
    tio->c_cc[VERASE] = 127;  /* DEL */
    tio->c_cc[VKILL] = 21;    /* Ctrl-U */
    tio->c_cc[VEOF] = 4;      /* Ctrl-D */
    tio->c_cc[VSTART] = 17;   /* Ctrl-Q */
    tio->c_cc[VSTOP] = 19;    /* Ctrl-S */
    tio->c_cc[VSUSP] = 26;    /* Ctrl-Z */
    tio->c_cc[VMIN] = 1;
    tio->c_cc[VTIME] = 0;
    cfsetispeed(tio, B38400);
    cfsetospeed(tio, B38400);
}

JNIEXPORT jobject JNICALL
Java_com_klix_code_data_sandbox_PtyBridge_nativeOpenAndForkPty(
        JNIEnv *env,
        jobject thiz,
        jobjectArray jargv,
        jobjectArray jenvp,
        jstring jcwd,
        jint rows,
        jint cols) {
    (void)thiz;

    jclass session_cls = (*env)->FindClass(env, "com/klix/code/data/sandbox/PtySession");
    if (!session_cls) return NULL;
    jmethodID session_ctor = (*env)->GetMethodID(env, session_cls, "<init>", "(II)V");
    if (!session_ctor) return NULL;

    if (!jargv || (*env)->GetArrayLength(env, jargv) <= 0) {
        return (*env)->NewObject(env, session_cls, session_ctor, (jint)-1, (jint)-1);
    }

    char **c_argv = copy_jstring_array(env, jargv);
    char **c_envp = copy_jstring_array(env, jenvp);
    char *c_cwd = NULL;
    if (jcwd) {
        const char *utf = (*env)->GetStringUTFChars(env, jcwd, NULL);
        if (utf) {
            c_cwd = strdup(utf);
            (*env)->ReleaseStringUTFChars(env, jcwd, utf);
        }
    }

    if (!c_argv || !c_envp || !c_argv[0]) {
        free_string_array(c_argv);
        free_string_array(c_envp);
        free(c_cwd);
        return (*env)->NewObject(env, session_cls, session_ctor, (jint)-1, (jint)-1);
    }

    struct termios tio;
    init_default_termios(&tio);

    struct winsize ws;
    memset(&ws, 0, sizeof(ws));
    ws.ws_row = (unsigned short)(rows > 0 ? rows : 24);
    ws.ws_col = (unsigned short)(cols > 0 ? cols : 80);

    int master_fd = -1;
    int slave_fd = -1;
    if (openpty(&master_fd, &slave_fd, NULL, &tio, &ws) != 0) {
        free_string_array(c_argv);
        free_string_array(c_envp);
        free(c_cwd);
        return (*env)->NewObject(env, session_cls, session_ctor, (jint)-1, (jint)-1);
    }

    fcntl(master_fd, F_SETFD, FD_CLOEXEC);

    pid_t pid = fork();
    if (pid < 0) {
        close(master_fd);
        close(slave_fd);
        free_string_array(c_argv);
        free_string_array(c_envp);
        free(c_cwd);
        return (*env)->NewObject(env, session_cls, session_ctor, (jint)-1, (jint)-1);
    }

    if (pid == 0) {
        close(master_fd);

        /* Reset signal mask and handlers inherited from ART/JVM */
        sigset_t empty_mask;
        sigemptyset(&empty_mask);
        sigprocmask(SIG_SETMASK, &empty_mask, NULL);
        struct sigaction sa;
        memset(&sa, 0, sizeof(sa));
        sa.sa_handler = SIG_DFL;
        sigaction(SIGINT, &sa, NULL);
        sigaction(SIGQUIT, &sa, NULL);
        sigaction(SIGTERM, &sa, NULL);
        sigaction(SIGHUP, &sa, NULL);
        sigaction(SIGPIPE, &sa, NULL);
        sigaction(SIGCHLD, &sa, NULL);

        setsid();
        ioctl(slave_fd, TIOCSCTTY, 0);
        tcsetpgrp(slave_fd, getpid());

        dup2(slave_fd, STDIN_FILENO);
        dup2(slave_fd, STDOUT_FILENO);
        dup2(slave_fd, STDERR_FILENO);
        if (slave_fd > STDERR_FILENO) {
            close(slave_fd);
        }

        if (c_cwd && c_cwd[0] != '\0') {
            if (chdir(c_cwd) != 0) {
                /* Continue to execve; binary/error surfaces via exit code */
            }
        }

        execve(c_argv[0], c_argv, c_envp);
        _exit(127);
    }

    /* Parent */
    close(slave_fd);
    int flags = fcntl(master_fd, F_GETFL, 0);
    if (flags >= 0) {
        fcntl(master_fd, F_SETFL, flags | O_NONBLOCK);
    }

    free_string_array(c_argv);
    free_string_array(c_envp);
    free(c_cwd);

    return (*env)->NewObject(env, session_cls, session_ctor, (jint)pid, (jint)master_fd);
}

JNIEXPORT jint JNICALL
Java_com_klix_code_data_sandbox_PtyBridge_nativeReadPty(
        JNIEnv *env,
        jobject thiz,
        jint master_fd,
        jbyteArray jbuffer) {
    (void)thiz;
    if (master_fd < 0 || !jbuffer) return 0;

    jsize cap = (*env)->GetArrayLength(env, jbuffer);
    if (cap <= 0) return 0;

    struct pollfd pfd;
    pfd.fd = master_fd;
    pfd.events = POLLIN | POLLHUP | POLLERR;
    pfd.revents = 0;

    int prc = poll(&pfd, 1, 50);
    if (prc == 0) {
        return -1; /* Timeout with no bytes yet */
    }
    if (prc < 0) {
        if (errno == EINTR) return -1;
        return 0;
    }

    jbyte stack_buf[4096];
    jbyte *tmp = stack_buf;
    jboolean allocated = JNI_FALSE;
    if (cap > (jsize)sizeof(stack_buf)) {
        tmp = (jbyte *)malloc((size_t)cap);
        if (!tmp) {
            tmp = stack_buf;
            cap = (jsize)sizeof(stack_buf);
        } else {
            allocated = JNI_TRUE;
        }
    }

    ssize_t n = read(master_fd, tmp, (size_t)cap);
    if (n > 0) {
        (*env)->SetByteArrayRegion(env, jbuffer, 0, (jsize)n, tmp);
        if (allocated) free(tmp);
        return (jint)n;
    }

    if (allocated) free(tmp);

    if (n == 0) {
        return 0;
    }
    if (errno == EAGAIN || errno == EWOULDBLOCK || errno == EINTR) {
        return (pfd.revents & (POLLHUP | POLLERR)) ? 0 : -1;
    }
    /* EIO (slave closed) or EBADF -> EOF */
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_klix_code_data_sandbox_PtyBridge_nativeWritePty(
        JNIEnv *env,
        jobject thiz,
        jint master_fd,
        jbyteArray jdata) {
    (void)thiz;
    if (master_fd < 0 || !jdata) return -1;

    jsize len = (*env)->GetArrayLength(env, jdata);
    if (len == 0) return 0;

    jbyte *bytes = (*env)->GetByteArrayElements(env, jdata, NULL);
    if (!bytes) return -1;

    jsize written = 0;
    while (written < len) {
        ssize_t n = write(master_fd, bytes + written, (size_t)(len - written));
        if (n > 0) {
            written += (jsize)n;
        } else if (n < 0 && errno == EINTR) {
            continue;
        } else if (n < 0 && (errno == EAGAIN || errno == EWOULDBLOCK)) {
            struct pollfd pfd;
            pfd.fd = master_fd;
            pfd.events = POLLOUT;
            pfd.revents = 0;
            if (poll(&pfd, 1, 200) <= 0) break;
        } else {
            if (written == 0) written = -1;
            break;
        }
    }

    (*env)->ReleaseByteArrayElements(env, jdata, bytes, JNI_ABORT);
    return (jint)written;
}

JNIEXPORT jint JNICALL
Java_com_klix_code_data_sandbox_PtyBridge_nativeResizePty(
        JNIEnv *env,
        jobject thiz,
        jint master_fd,
        jint rows,
        jint cols) {
    (void)env;
    (void)thiz;
    if (master_fd < 0 || rows <= 0 || cols <= 0) return -1;

    struct winsize ws;
    memset(&ws, 0, sizeof(ws));
    ws.ws_row = (unsigned short)rows;
    ws.ws_col = (unsigned short)cols;
    return ioctl(master_fd, TIOCSWINSZ, &ws) == 0 ? 0 : -1;
}

JNIEXPORT jint JNICALL
Java_com_klix_code_data_sandbox_PtyBridge_nativeSendSignal(
        JNIEnv *env,
        jobject thiz,
        jint pid,
        jint signal) {
    (void)env;
    (void)thiz;
    if (pid <= 0 || signal <= 0) return -1;

    int rc_pg = kill((pid_t)(-pid), signal);
    int rc_p = kill((pid_t)pid, signal);
    return (rc_pg == 0 || rc_p == 0) ? 0 : -1;
}

JNIEXPORT jint JNICALL
Java_com_klix_code_data_sandbox_PtyBridge_nativeWaitPid(
        JNIEnv *env,
        jobject thiz,
        jint pid) {
    (void)env;
    (void)thiz;
    if (pid <= 0) return -1;

    int status = 0;
    pid_t rc;
    do {
        rc = waitpid((pid_t)pid, &status, 0);
    } while (rc < 0 && errno == EINTR);

    if (rc < 0) return -1;
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    if (WIFSIGNALED(status)) return 128 + WTERMSIG(status);
    return -1;
}

JNIEXPORT void JNICALL
Java_com_klix_code_data_sandbox_PtyBridge_nativeCloseFd(
        JNIEnv *env,
        jobject thiz,
        jint master_fd) {
    (void)env;
    (void)thiz;
    if (master_fd >= 0) {
        close(master_fd);
    }
}
