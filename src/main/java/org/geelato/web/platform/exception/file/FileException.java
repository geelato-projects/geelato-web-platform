package org.geelato.web.platform.exception.file;

import org.geelato.core.exception.CoreException;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/10/25 11:28
 */
public class FileException extends CoreException {
    private static final String MESSAGE = "12 File Exception";
    private static final int CODE = 1200;

    public FileException() {
        super(MESSAGE, CODE);
    }

    public FileException(String msg, int code) {
        super(msg, code);
    }

    public FileException(String detailMessage) {
        super(String.format("%s：%s", MESSAGE, detailMessage), CODE);
    }
}
