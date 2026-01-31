// Sorry for the inconsistent use of single and double quotation marks

document.addEventListener('DOMContentLoaded', function() {
    // Input Mode Selection
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


    // Starting Cash Input Formatting
    const startingCashInput = document.getElementById("starting-cash-input")
    const currencyFormatter = new Intl.NumberFormat("en-US", {style: "currency", currency: "USD", minimumFractionDigits: 0});

    // Format into currency when page is loaded
    startingCashInput.value = currencyFormatter.format(Number(startingCashInput.value));

    // On blur, format to currency
    startingCashInput.addEventListener("blur", function(e) {
    if (/^[\d.-]+$/.test(e.target.value)) { // regex in JS is between two slashes
        e.target.value = currencyFormatter.format(Number(e.target.value));
    }
   });

   // On click, remove contents
   startingCashInput.addEventListener("click", function(e) {
     e.target.value = '';
   });

   // Fill manual textarea with example CSV when button clicked
   const fillExampleButton = document.getElementById('fill-example-button');
   const examplePre = document.querySelector('.example-data');
   const manualTextarea = document.querySelector('.manual-input textarea');
   if (fillExampleButton && examplePre && manualTextarea) {
       fillExampleButton.addEventListener('click', function(e) {
           // Use the text inside the hidden <pre> (preserves newlines)
           manualTextarea.value = examplePre.innerText;
        //    // Make sure manual input is visible (in case JS toggling changed state)
        //    manualInput.classList.remove('hidden');
        //    fileInput.classList.add('hidden');
           // Focus the textarea so user can review/edit
           manualTextarea.focus();
       });
   }

});