/*
 * Esko Piirainen, Luomus, 2013
 */
(function($){
    $.fn.extend({
        collapse: function(options) {
            var defaults = {
                fadeTime: 500,
                showMoreText: '+ N&auml;yt&auml; lis&auml;&auml;'
            }
            var options = $.extend(defaults, options);
            
        	return this.each(function() {
            	var collapseElement = $(this);
            	collapseElement.addClass('collapse');
      			var collapseFadeContainer = $('<div class="collapseFadeContainer" />');
      			var showMoreButton = $('<a href="#" class="showFullContentButton">'+options.showMoreText+'</a>').button();
      			$(this).append(collapseFadeContainer);
      			$(this).append(showMoreButton);
      			showMoreButton.on('click', function() {
        			$(this).blur();
        			$(this).fadeOut(options.fadeTime);
        			collapseElement.removeClass('collapse', options.fadeTime);
        			$(".collapseFadeContainer").remove();
        			return false;
      			});
        	});
    	}
    });
})(jQuery);
