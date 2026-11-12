param([string]$BaseUrl = 'http://localhost:8080/api/v1')
$ErrorActionPreference = 'Stop'
function Api($Method, $Path, $Token, $Body) {
    $requestArgs = @{ Method=$Method; Uri="$BaseUrl$Path" }
    if ($Token) { $requestArgs.Headers=@{Authorization="Bearer $Token"} }
    if ($null -ne $Body) { $requestArgs.ContentType='application/json'; $requestArgs.Body=($Body | ConvertTo-Json -Depth 8) }
    Invoke-RestMethod @requestArgs
}
$suffix=[guid]::NewGuid().ToString('N')
$password='LocalDemo123!'
$businessEmail="bakery-$suffix@example.com"
$clientEmail="client-$suffix@example.com"
$null=Api POST /auth/register $null @{
    email=$businessEmail; password=$password; role='BUSINESS'
    businessName='Demo Bakery'; pickupLocation='Cluj-Napoca, Strada Exemplu 1'
}
$business=Api POST /auth/login $null @{email=$businessEmail; password=$password}
$offer=Api POST /offers $business.accessToken @{
    productName='Croissant rescue bag'; category='Bakery'; description='Fresh croissants'
    originalPrice=8.00; wasteLessPrice=3.20; quantity=3
    pickupStart=[DateTimeOffset]::Now.AddHours(1).ToString('o')
    pickupEnd=[DateTimeOffset]::Now.AddHours(3).ToString('o')
}
$null=Api POST /auth/register $null @{email=$clientEmail; password=$password; role='CLIENT'}
$client=Api POST /auth/login $null @{email=$clientEmail; password=$password}
$listing=Api GET '/offers?page=0&size=20' $client.accessToken $null
$reservation=Api POST /reservations $client.accessToken @{offerId=$offer.id; quantity=2}
$after=Api GET "/offers/$($offer.id)" $client.accessToken $null
if ($after.quantity -ne 1 -or $reservation.total -ne 6.40 -or $reservation.status -ne 'RESERVED') {
    throw 'Unexpected reservation or stock result'
}
[pscustomobject]@{
    OfferId=$offer.id; ReservationId=$reservation.id; StockBefore=$offer.quantity
    StockAfter=$after.quantity; Total=$reservation.total; Currency=$reservation.currency
    Status=$reservation.status; AvailableOfferCount=$listing.totalElements
} | Format-List
