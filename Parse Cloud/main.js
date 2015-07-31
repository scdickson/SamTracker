
// Use Parse.Cloud.define to define as many cloud functions as you want.
// For example:
Parse.Cloud.afterSave("LocationObject", function(request, response) {
  var lat = request.object.get("lat");
  var lng = request.object.get("lng");
   
   Parse.Cloud.httpRequest({
                method: "POST",
                url: 'https://maps.googleapis.com/maps/api/geocode/json',
                params: {
                    latlng : lat + "," + lng,
                    key: "AIzaSyCazMxcpH4l4HSwB_ofk8Nnm9aLTnAVyQI"
                },
                success: function(httpResponse) {
                    var response=httpResponse.data;
                    if(response.status == "OK"){
						var formattedAddress = response.results[0].formatted_address;
						request.object.set("formattedAddress", formattedAddress);
						request.object.save();
						response.success();
						console.log(formattedAddress);
                     }
                },
                error: function(httpResponse) {
                    console.error('Request failed with response code ' + httpResponse.status);
                }
            });
});
