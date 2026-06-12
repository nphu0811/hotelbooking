(function () {
    var dayMs = 24 * 60 * 60 * 1000;
    var destinations = ["TP. Hồ Chí Minh", "Vũng Tàu", "Nha Trang", "Đà Lạt", "Đà Nẵng"];
    var weekdayLabels = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];
    var monthFormatter = new Intl.DateTimeFormat("vi-VN", { month: "long", year: "numeric" });

    function onReady(callback) {
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", callback);
        } else {
            callback();
        }
    }

    function normalize(value) {
        return (value || "")
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .replace(/đ/g, "d")
            .replace(/Đ/g, "D")
            .toLowerCase()
            .trim();
    }

    function isoDate(date) {
        return [
            date.getFullYear(),
            String(date.getMonth() + 1).padStart(2, "0"),
            String(date.getDate()).padStart(2, "0")
        ].join("-");
    }

    function parseDate(value) {
        if (!value) return null;
        var match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
        if (!match) return null;
        return new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
    }

    function addDays(date, days) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
    }

    function sameDay(first, second) {
        return first && second && isoDate(first) === isoDate(second);
    }

    function dateLabel(date) {
        return weekdayLabels[date.getDay()] + ", " + date.getDate() + " tháng " + (date.getMonth() + 1);
    }

    function intValue(input, fallback) {
        var value = Number.parseInt(input && input.value, 10);
        return Number.isFinite(value) ? value : fallback;
    }

    function initWidget(root) {
        var form = root.querySelector(".hero-search");
        var destinationInput = root.querySelector("[data-destination-input]");
        var clearDestination = root.querySelector("[data-clear-destination]");
        var dateDisplay = root.querySelector("[data-date-display]");
        var guestsDisplay = root.querySelector("[data-guests-display]");
        var checkInInput = root.querySelector("[data-check-in]");
        var checkOutInput = root.querySelector("[data-check-out]");
        var guestsInput = root.querySelector("[data-guests-input]");
        var panels = root.querySelectorAll("[data-search-panel]");
        var calendarMonths = root.querySelector("[data-calendar-months]");
        var petToggle = root.querySelector("[data-pet-toggle]");
        if (!form || !destinationInput || !dateDisplay || !guestsDisplay) return;

        var tomorrow = addDays(new Date(), 1);
        var state = {
            visibleMonth: parseDate(checkInInput && checkInInput.value) || tomorrow,
            checkIn: parseDate(checkInInput && checkInInput.value),
            checkOut: parseDate(checkOutInput && checkOutInput.value),
            adults: intValue(root.querySelector('[data-counter-input="adults"]'), 2),
            children: intValue(root.querySelector('[data-counter-input="children"]'), 0),
            rooms: intValue(root.querySelector('[data-counter-input="rooms"]'), 1)
        };

        if (!state.checkIn && checkInInput && !checkInInput.value) {
            state.checkIn = null;
        }
        if (state.checkIn && !state.checkOut && checkOutInput && checkOutInput.value) {
            state.checkOut = parseDate(checkOutInput.value);
        }

        function panel(name) {
            return root.querySelector('[data-search-panel="' + name + '"]');
        }

        function closePanels() {
            panels.forEach(function (item) {
                item.hidden = true;
            });
        }

        function openPanel(name) {
            panels.forEach(function (item) {
                item.hidden = item.getAttribute("data-search-panel") !== name;
            });
            if (name === "dates") {
                renderCalendar();
            }
        }

        function updateDestinationClear() {
            if (clearDestination) {
                clearDestination.hidden = destinationInput.value.trim() === "";
            }
        }

        function updateDateFields() {
            if (state.checkIn) {
                checkInInput.value = isoDate(state.checkIn);
            } else if (checkInInput) {
                checkInInput.value = "";
            }

            if (state.checkOut) {
                checkOutInput.value = isoDate(state.checkOut);
            } else if (checkOutInput) {
                checkOutInput.value = "";
            }

            if (state.checkIn && state.checkOut) {
                dateDisplay.value = dateLabel(state.checkIn) + " - " + dateLabel(state.checkOut);
            } else if (state.checkIn) {
                dateDisplay.value = dateLabel(state.checkIn) + " - Trả phòng";
            } else {
                dateDisplay.value = "Nhận phòng - Trả phòng";
            }
        }

        function updateGuestFields() {
            var counterInputs = {
                adults: root.querySelector('[data-counter-input="adults"]'),
                children: root.querySelector('[data-counter-input="children"]'),
                rooms: root.querySelector('[data-counter-input="rooms"]')
            };
            Object.keys(counterInputs).forEach(function (key) {
                var input = counterInputs[key];
                var display = root.querySelector('[data-counter-value="' + key + '"]');
                if (input) input.value = state[key];
                if (display) display.textContent = state[key];
            });
            if (guestsInput) {
                guestsInput.value = state.adults + state.children;
            }
            guestsDisplay.value = state.adults + " người lớn · " + state.children + " trẻ em · " + state.rooms + " phòng";
        }

        function renderDestinations() {
            var query = normalize(destinationInput.value);
            panel("destination").querySelectorAll("[data-destination]").forEach(function (button) {
                var value = button.getAttribute("data-destination");
                button.hidden = query && normalize(value).indexOf(query) === -1;
            });
        }

        function renderMonth(monthDate) {
            var first = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
            var lastDate = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0).getDate();
            var mondayOffset = (first.getDay() + 6) % 7;
            var html = '<section class="calendar-month"><h3>' + monthFormatter.format(first) + '</h3>';
            html += '<div class="calendar-weekdays"><span>T2</span><span>T3</span><span>T4</span><span>T5</span><span>T6</span><span>T7</span><span>CN</span></div>';
            html += '<div class="calendar-days">';
            for (var blank = 0; blank < mondayOffset; blank++) {
                html += '<span class="calendar-empty"></span>';
            }
            for (var day = 1; day <= lastDate; day++) {
                var date = new Date(first.getFullYear(), first.getMonth(), day);
                var inRange = state.checkIn && state.checkOut && date > state.checkIn && date < state.checkOut;
                var classes = [];
                if (sameDay(date, state.checkIn)) classes.push("is-start");
                if (sameDay(date, state.checkOut)) classes.push("is-end");
                if (inRange) classes.push("is-range");
                html += '<button type="button" class="' + classes.join(" ") + '" data-calendar-date="' + isoDate(date) + '">' + day + '</button>';
            }
            html += '</div></section>';
            return html;
        }

        function renderCalendar() {
            if (!calendarMonths) return;
            var firstMonth = new Date(state.visibleMonth.getFullYear(), state.visibleMonth.getMonth(), 1);
            var secondMonth = new Date(firstMonth.getFullYear(), firstMonth.getMonth() + 1, 1);
            calendarMonths.innerHTML = renderMonth(firstMonth) + renderMonth(secondMonth);
        }

        destinationInput.addEventListener("focus", function () {
            renderDestinations();
            openPanel("destination");
        });
        destinationInput.addEventListener("click", function () {
            renderDestinations();
            openPanel("destination");
        });
        destinationInput.addEventListener("input", function () {
            updateDestinationClear();
            renderDestinations();
            openPanel("destination");
        });

        if (clearDestination) {
            clearDestination.addEventListener("click", function (event) {
                event.preventDefault();
                event.stopPropagation();
                destinationInput.value = "";
                updateDestinationClear();
                renderDestinations();
                destinationInput.focus();
            });
        }

        root.querySelectorAll("[data-destination]").forEach(function (button) {
            button.addEventListener("click", function () {
                destinationInput.value = button.getAttribute("data-destination");
                updateDestinationClear();
                closePanels();
            });
        });

        dateDisplay.addEventListener("click", function () {
            openPanel("dates");
        });
        dateDisplay.addEventListener("focus", function () {
            openPanel("dates");
        });
        guestsDisplay.addEventListener("click", function () {
            openPanel("guests");
        });
        guestsDisplay.addEventListener("focus", function () {
            openPanel("guests");
        });

        root.querySelector("[data-calendar-prev]")?.addEventListener("click", function () {
            state.visibleMonth = new Date(state.visibleMonth.getFullYear(), state.visibleMonth.getMonth() - 1, 1);
            renderCalendar();
        });
        root.querySelector("[data-calendar-next]")?.addEventListener("click", function () {
            state.visibleMonth = new Date(state.visibleMonth.getFullYear(), state.visibleMonth.getMonth() + 1, 1);
            renderCalendar();
        });
        if (calendarMonths) {
            calendarMonths.addEventListener("click", function (event) {
                var button = event.target.closest("[data-calendar-date]");
                if (!button) return;
                var selected = parseDate(button.getAttribute("data-calendar-date"));
                if (!state.checkIn || state.checkOut || selected <= state.checkIn) {
                    state.checkIn = selected;
                    state.checkOut = null;
                } else {
                    state.checkOut = selected;
                    closePanels();
                }
                updateDateFields();
                renderCalendar();
            });
        }

        root.querySelectorAll("[data-counter-action]").forEach(function (button) {
            button.addEventListener("click", function () {
                var key = button.getAttribute("data-counter-target");
                var action = button.getAttribute("data-counter-action");
                var min = key === "adults" || key === "rooms" ? 1 : 0;
                var max = key === "rooms" ? 8 : 30;
                var next = state[key] + (action === "increase" ? 1 : -1);
                state[key] = Math.min(max, Math.max(min, next));
                updateGuestFields();
            });
        });

        if (petToggle) {
            petToggle.addEventListener("click", function () {
                var pressed = petToggle.getAttribute("aria-pressed") === "true";
                petToggle.setAttribute("aria-pressed", String(!pressed));
            });
        }

        root.querySelectorAll("[data-close-popovers]").forEach(function (button) {
            button.addEventListener("click", closePanels);
        });

        form.addEventListener("submit", function () {
            if (state.checkIn && !state.checkOut) {
                state.checkOut = addDays(state.checkIn, 1);
            }
            updateDateFields();
            updateGuestFields();
        });

        document.addEventListener("click", function (event) {
            if (!root.contains(event.target)) {
                closePanels();
            }
        });
        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                closePanels();
            }
        });

        updateDestinationClear();
        updateDateFields();
        updateGuestFields();
        renderCalendar();
    }

    onReady(function () {
        document.querySelectorAll("[data-search-widget]").forEach(initWidget);
    });
})();
