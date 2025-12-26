document.addEventListener('DOMContentLoaded', function() {
  // Select all divs with a data-plot attribute
  const chartDivs = document.querySelectorAll('[data-plot]');
  
  chartDivs.forEach(div => {
    const plotData = JSON.parse(div.dataset.plot);
    Plotly.newPlot(div.id, [plotData]);
  });
});