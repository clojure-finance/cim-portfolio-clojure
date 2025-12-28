document.addEventListener('DOMContentLoaded', function() {
    const radios = document.querySelectorAll('input[name=\"input-mode\"]');
    const manualInput = document.querySelector('.manual-input');
    const fileInput = document.querySelector('.file-input');
    function toggleInputs() {
        const selectedMode = document.querySelector('input[name=\"input-mode\"]:checked').value;
            if (selectedMode === 'manual') {
                manualInput.classList.remove('hidden');
                fileInput.classList.add('hidden');
                // clear file input when switching to manual
                const fileField = fileInput.querySelector('input[type="file"]');
                if (fileField) fileField.value = '';
            } else {
                manualInput.classList.add('hidden');
                fileInput.classList.remove('hidden');
                // clear manual textarea when switching to file
                const ta = manualInput.querySelector('textarea');
                if (ta) ta.value = '';
            }
            }
    
    radios.forEach(radio => {
        radio.addEventListener('change', toggleInputs);
    });
    
    toggleInputs();
});