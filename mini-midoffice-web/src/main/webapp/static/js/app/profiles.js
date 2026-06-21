/* =========================================================
   profiles.js — list/search, create modal, edit modal
   ========================================================= */
var Profiles = (function ($) {
    'use strict';

    var tpl;
    var currentEditId = null;
    var eventsWired = false;

    function init() {
        tpl = Handlebars.compile($('#tpl-profiles').html());
        if (!eventsWired) {
            bindEvents();
            eventsWired = true;
        }
        load();
    }

    function load(q) {
        $('#profiles-tbody').html(
            '<tr><td colspan="6" class="text-muted text-center">' +
            '<span class="glyphicon glyphicon-refresh spinning"></span> Loading…</td></tr>'
        );
        API.profiles.list(q).done(function (profiles) {
            $('#profiles-tbody').html(tpl({profiles: profiles}));
        });
    }

    function bindEvents() {
        /* Search */
        $('#profiles-search-btn').on('click', function () {
            load($('#profiles-search-input').val().trim() || undefined);
        });
        $('#profiles-search-input').on('keypress', function (e) {
            if (e.which === 13) {
                load($(this).val().trim() || undefined);
            }
        });

        /* New profile */
        $('#btn-new-profile').on('click', function () {
            openModal(null);
        });

        /* Form submit (create or update) */
        $('#profile-form').on('submit', function (e) {
            e.preventDefault();
            saveProfile();
        });

        /* Edit — delegated, because rows are rendered dynamically */
        $(document).on('click', '.btn-edit-profile', function () {
            openModal($(this).data('id'));
        });

        /* View bookings from profiles table */
        $(document).on('click', '.btn-view-bookings', function () {
            var tid = $(this).data('id');
            Bookings.loadForTraveller(tid);
            $('[data-section="bookings"]').trigger('click');
        });
    }

    function openModal(id) {
        currentEditId = id;
        var $form = $('#profile-form');
        $form[0].reset();

        if (id) {
            $('#profile-modal-title').text('Edit Profile #' + id);
            API.profiles.get(id).done(function (p) {
                $form.find('[name="firstName"]').val(p.firstName || '');
                $form.find('[name="lastName"]').val(p.lastName || '');
                $form.find('[name="email"]').val(p.email || '');
                $form.find('[name="company"]').val(p.company || '');
                $form.find('[name="passportNumber"]').val(p.passportNumber || '');
                $form.find('[name="frequentFlyerNumber"]').val(p.frequentFlyerNumber || '');
            });
        } else {
            $('#profile-modal-title').text('New Profile');
        }

        $('#profile-modal').modal('show');
    }

    function saveProfile() {
        var data = {
            firstName:           $('#profile-form [name="firstName"]').val().trim(),
            lastName:            $('#profile-form [name="lastName"]').val().trim(),
            email:               $('#profile-form [name="email"]').val().trim(),
            company:             $('#profile-form [name="company"]').val().trim() || null,
            passportNumber:      $('#profile-form [name="passportNumber"]').val().trim() || null,
            frequentFlyerNumber: $('#profile-form [name="frequentFlyerNumber"]').val().trim() || null
        };

        var promise = currentEditId
            ? API.profiles.update(currentEditId, data)
            : API.profiles.create(data);

        promise.done(function () {
            $('#profile-modal').modal('hide');
            load();
            window.showAlert(
                currentEditId ? 'Profile updated successfully.' : 'Profile created successfully.',
                'success'
            );
        });
    }

    return {init: init};

}(jQuery));
