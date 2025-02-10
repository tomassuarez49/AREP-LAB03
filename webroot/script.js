async function fetchData() {
    try {
        let response = await fetch('http://localhost:32000/data');
        if (!response.ok) {
            throw new Error('Error en la solicitud');
        }
        let data = await response.json();
        document.getElementById("result").innerText = JSON.stringify(data); // Muestra los datos
    } catch (error) {
        document.getElementById("result").innerText = 'Error al obtener los datos: ' + error.message;
    }
}
