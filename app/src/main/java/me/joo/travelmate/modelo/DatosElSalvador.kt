package me.joo.travelmate.modelo

data class DepartamentoSV(val nombre: String, val latitud: Double, val longitud: Double, val radioMetros: Int)

object DatosElSalvador {
    const val RECT_PAIS = "-90.1287,13.1490,-87.6929,14.4510"

    val departamentos: List<DepartamentoSV> = listOf(
        DepartamentoSV("Ahuachapán", 13.9214, -89.8450, 22000),
        DepartamentoSV("Cabañas", 13.8644, -88.7492, 20000),
        DepartamentoSV("Chalatenango", 14.0333, -88.9333, 28000),
        DepartamentoSV("Cuscatlán", 13.7364, -88.9314, 18000),
        DepartamentoSV("La Libertad", 13.4883, -89.3222, 28000),
        DepartamentoSV("La Paz", 13.4842, -88.9833, 20000),
        DepartamentoSV("La Unión", 13.3369, -87.8439, 28000),
        DepartamentoSV("Morazán", 13.7667, -88.1000, 22000),
        DepartamentoSV("San Miguel", 13.4833, -88.1833, 28000),
        DepartamentoSV("San Salvador", 13.6929, -89.2182, 25000),
        DepartamentoSV("San Vicente", 13.6333, -88.8000, 20000),
        DepartamentoSV("Santa Ana", 13.9942, -89.5592, 28000),
        DepartamentoSV("Sonsonate", 13.7189, -89.7242, 22000),
        DepartamentoSV("Usulután", 13.3500, -88.4500, 28000)
    )

    val categorias: Map<String, String> = linkedMapOf(
        "Todas" to "tourism,catering.restaurant,entertainment.museum,leisure.park,accommodation.hotel,catering.cafe,beach,commercial.shopping_mall,building.historic,heritage,natural",
        "Restaurante" to "catering.restaurant",
        "Museo" to "entertainment.museum",
        "Parque" to "leisure.park",
        "Atracción turística" to "tourism.attraction,tourism.sights",
        "Hotel" to "accommodation.hotel",
        "Café" to "catering.cafe",
        "Playa" to "beach",
        "Centro comercial" to "commercial.shopping_mall",
        "Sitio histórico" to "building.historic,heritage",
        "Naturaleza" to "natural"
    )
}
