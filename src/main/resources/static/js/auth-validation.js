(function () {
    "use strict";

    var emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    var phonePattern = /^(0|\+84)\d{9,10}$/;
    var strongPasswordPattern = /^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

    function fieldError(form, name, rule) {
        var nodes = form.querySelectorAll("[data-field-error]");
        for (var i = 0; i < nodes.length; i += 1) {
            var attr = nodes[i].getAttribute("data-field-error");
            if (attr === name || (rule && attr === rule) || (name === "username" && attr === "email")) {
                return nodes[i];
            }
        }
        return null;
    }

    function setError(form, input, message) {
        input.classList.toggle("is-invalid", Boolean(message));
        input.setAttribute("aria-invalid", message ? "true" : "false");
        var rule = input.getAttribute("data-validate-field");
        var target = fieldError(form, input.name, rule);
        if (target) {
            target.textContent = message || "";
        }
    }

    function valueOf(form, name) {
        var input = form.elements[name];
        return input ? String(input.value || "") : "";
    }

    function validateInput(form, input) {
        var value = String(input.value || "").trim();
        var rule = input.getAttribute("data-validate-field");
        var message = "";

        if (rule === "identifier") {
            if (!value) {
                message = "Vui lòng nhập email hoặc số điện thoại.";
            } else if (value.indexOf("@") >= 0 && !emailPattern.test(value)) {
                message = "Định dạng email không hợp lệ.";
            } else if (value.indexOf("@") < 0 && !phonePattern.test(value)) {
                message = "Số điện thoại Việt Nam không hợp lệ.";
            }
        }

        if (rule === "email") {
            if (!value) {
                message = "Vui lòng nhập email.";
            } else if (!emailPattern.test(value)) {
                message = "Định dạng email không hợp lệ.";
            }
        }

        if (rule === "phone") {
            if (!value) {
                message = "Vui lòng nhập số điện thoại.";
            } else if (!phonePattern.test(value)) {
                message = "Số điện thoại Việt Nam không hợp lệ.";
            }
        }

        if (rule === "fullName") {
            if (!value) {
                message = "Vui lòng nhập họ tên.";
            } else if (value.length < 2 || value.length > 100) {
                message = "Họ tên phải từ 2 đến 100 ký tự.";
            }
        }

        if (rule === "password") {
            if (!value) {
                message = "Vui lòng nhập mật khẩu.";
            } else if (value.length < 8) {
                message = "Mật khẩu yêu cầu phải có ít nhất 8 ký tự.";
            }
        }

        if (rule === "strongPassword") {
            if (!value) {
                message = "Vui lòng nhập mật khẩu.";
            } else if (!strongPasswordPattern.test(value)) {
                message = "Mật khẩu cần ít nhất 8 ký tự, 1 chữ hoa, 1 số và 1 ký tự đặc biệt.";
            }
        }

        if (rule === "optionalStrongPassword" && value && !strongPasswordPattern.test(value)) {
            message = "Mật khẩu mới cần ít nhất 8 ký tự, 1 chữ hoa, 1 số và 1 ký tự đặc biệt.";
        }

        if (rule === "currentPassword" && valueOf(form, "newPassword").trim() && !value) {
            message = "Vui lòng nhập mật khẩu hiện tại để đổi mật khẩu.";
        }

        if (rule === "confirmPassword") {
            if (!value) {
                message = "Vui lòng xác nhận mật khẩu.";
            } else if (value !== valueOf(form, input.getAttribute("data-match-field"))) {
                message = "Xác nhận mật khẩu không khớp.";
            }
        }

        if (rule === "optionalConfirmPassword") {
            var newPassword = valueOf(form, input.getAttribute("data-match-field"));
            if (newPassword && !value) {
                message = "Vui lòng xác nhận mật khẩu mới.";
            } else if (newPassword && value !== newPassword) {
                message = "Xác nhận mật khẩu mới không khớp.";
            }
        }

        if (rule === "otp") {
            if (!value) {
                message = "Vui lòng nhập mã OTP.";
            } else if (!/^\d{6}$/.test(value)) {
                message = "Mã OTP phải gồm đúng 6 chữ số.";
            }
        }

        setError(form, input, message);
        return !message;
    }

    function validateForm(form) {
        var inputs = form.querySelectorAll("[data-validate-field]");
        var valid = true;
        var firstInvalid = null;
        for (var i = 0; i < inputs.length; i += 1) {
            if (!validateInput(form, inputs[i])) {
                valid = false;
                if (!firstInvalid) {
                    firstInvalid = inputs[i];
                }
            }
        }
        if (firstInvalid) {
            firstInvalid.focus();
        }
        return valid;
    }

    document.querySelectorAll("form[data-validate]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (!validateForm(form)) {
                event.preventDefault();
            } else {
                // Disable submit button and show loading state to prevent double submits (fixes concurrent 403 Forbidden)
                var submitBtn = form.querySelector("button[type='submit']");
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.classList.add("is-loading");
                    submitBtn.innerHTML = '<span class="spinner"></span> Đang xử lý...';
                    
                    // Also disable other buttons inside the same form to prevent stray clicks
                    form.querySelectorAll("button").forEach(function (btn) {
                        if (btn !== submitBtn) {
                            btn.disabled = true;
                        }
                    });
                }
            }
        });
        form.querySelectorAll("[data-validate-field]").forEach(function (input) {
            input.addEventListener("blur", function () {
                validateInput(form, input);
            });
            input.addEventListener("input", function () {
                if (input.classList.contains("is-invalid")) {
                    validateInput(form, input);
                }
            });
        });
    });
}());
