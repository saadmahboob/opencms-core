(function($){
    /**
     * The OpenCms select-box jQuery plugin.
     * 
     * Takes a classic select-box and replaces it with a styled version.
     * 
     * Use following options:
     * open: function(elem) called when select-box is opened
     * select: function(this, elem, value) called when a value is selected
     * useHeight: boolean    if true, uses the current selector height to position the selectbox items
     * 
     * The following methods can be called by $(selector).selectBox('method name', additionalValue):
     * 'getValue': returns the currently selected value 
     * 'getIndex': returns the currently selected index
     * 'setValue': sets the value to additionalValue
     * 'setEnabled': enables/disables the selectbox if additionalValue true/false
     * 
     * The following styles are necessary:
     * <style>
     *	div.cms-selectbox{
     *   		position:relative; 
     *          display:inline-block;
     *   		line-height: 16px;
     *   		width: 100px;
     *   	}
     *   	
     *   	div.cms-selectbox span.cms-select-option{
     *   		display:block;
     *   		padding: 2px 5px 3px;
     *   		border:none;
     *   	}
     *  	
     *   	div.cms-selector{
     *   		width: 100px;
     *   		position:absolute;
     *   		top:20px;
     *   		left:-1px;
     *   		display:none;
     *   	}
     *   	
     *   	div.cms-open div.cms-selector{
     *   		display: block;
     *   	}
     *   
     *   </style>
     *   <!--[if IE 7]>
     *   <style>
     *   div.display{
     *       zoom: 1;
     *       display: inline;
     *       }
     *   </style>
     *   <![endif]-->
     * 
     * 
     * @param {Object} options the options object or the method string
     * @param {Object} additional the additional value object
     */
$.fn.selectBox=function(options, additional){
		var self=this;
        var opts={};
        if (typeof options == 'string' && options.indexOf('_')!=0){
            return eval(options+'(additional)');
        }
        
		opts=$.extend({}, $.fn.selectBox.defaults, options);
		_init();
		return self;
		
        function _init(){
			self.each(function(){
                
				var replacer=_generateReplacer(_getValues($(this)));
                replacer.insertBefore(this);
            }).hide();
			$(document.body).click(_close);
		}
		
		function _start(){
            var $this=$(this);
            var selectbox=$this.data('selectbox');
            $this.toggleClass('cms-open');
            if ($this.hasClass('cms-open')) {
                if (selectbox.opts.appendTo){
                    
                    var offset=$this.offset();
                    if (($.isFunction(selectbox.opts.selectorPosition) && selectbox.opts.selectorPosition() == 'top') || selectbox.opts.selectorPosition == 'top'){
                        offset.top-=21 * selectbox.valueCount;
                        selectbox.selector.removeClass('ui-corner-bottom').addClass('ui-corner-top')
                    }else{
                        offset.top+=20;
                    }
                    selectbox.selector.offset(offset).css('display', 'block');
                }
                if (selectbox.opts.useHeight) {
                    var $currentValue = selectbox.replacer.find('.cms-current-value');
                    var $selector = selectbox.selector;
                    $selector.css('top', $currentValue.outerHeight());
                }

                if ($.isFunction(selectbox.opts.open)) {
                    selectbox.opts.open(this);
                }

            }else{
                _close();
            }
            return false;
        }
        
		function _close(){
           $('div.cms-selectbox').removeClass('cms-open');
           if (opts.appendTo) {
               $('div.cms-selector').css('display', 'none');
           }
        }
        
		function _select($this, selectbox){
            var valueOptions=selectbox.selector.find('.cms-select-option');
            var index = valueOptions.index($this);
            var value=$this.attr('rel');
            var $currentValue = selectbox.replacer.find('span.cms-current-value'); 
            $currentValue.html($this.html()).attr('rel',value);
            
            _close();
            if ($.isFunction(selectbox.opts.select)){
                selectbox.opts.select($this.get(0), selectbox.replacer, value, index);
            }
            return false;
        }
        
        function _getValues(select){
            result=[];
            select.find('option').each(function(){
                result.push({value: $(this).val(), title: $(this).text()});
            });
            return result;
        }
        
        function _getValue(selectbox){
            return selectbox.replacer.find('span.cms-current-value').attr('rel');
        }
        
        function _getIndex(selectbox){
            if (selectbox!=null) {
                var valueOptions=$('.cms-select-option', selectbox.selector);
                var currentValue=valueOptions.filter('.cms-select-option[rel="'+_getValue(selectbox)+'"]');
                return valueOptions.index(currentValue);
            }
            return -1;
        }
        
        function _generateReplacer(values){
            var selectbox={};
            selectbox.replacer=$('<div class="cms-selectbox ui-state-default ui-corner-all"><span class="cms-select-opener ui-icon ui-icon-triangle-1-s"></span></div>')
			$('<span class="cms-current-value cms-select-option" unselectable="on"></span>').appendTo(selectbox.replacer).html(values[0].title).attr('rel', values[0].value);
			selectbox.selector=_generateSelector(selectbox, values);
            if (!opts.appendTo) {
                selectbox.selector.appendTo(selectbox.replacer);
                if (($.isFunction(opts.selectorPosition) && opts.selectorPosition() == 'top') || opts.selectorPosition == 'top') {
                    var top = 21 * values.length;
                    selectbox.selector.removeClass('ui-corner-bottom').addClass('ui-corner-top').css('top', '-' + top + 'px')
                }
            }else{
                var selectorParent= $(opts.appendTo);
                if (!selectorParent.length){
                    selectorParent=$(document.body);
                }
                selectbox.selector.appendTo(selectorParent)
            }
			selectbox.replacer.click(_start);
            $('span.cms-select-option', selectbox.replacer).andSelf().hover(function(){$(this).addClass('ui-state-hover');}, function(){$(this).removeClass('ui-state-hover')});
            
            selectbox.valueCount= values.length;
            selectbox.values=values;
            selectbox.opts=opts;
            selectbox.replacer.data('selectbox', selectbox);
            if (opts.width){
                selectbox.replacer.width(opts.width);
                selectbox.selector.width(opts.width);
            }
            return selectbox.replacer;    
        }
        
        function _generateSelector(selectbox, values){
            var selector=$('<div class="cms-selector ui-widget-content ui-corner-bottom"></div>');
			for (i=0; i<values.length; i++){
				$('<span/>', {
                    'class': "cms-select-option", 
                    'rel': values[i].value,
                    'unselectable': 'on', 
                    'html': values[i].title, 
                    'click': function(){
                        return _select($(this), selectbox);
                    }
                }).appendTo(selector);
            }
            return selector;
        }
        
        function generate(options){
            opts=$.extend({}, $.fn.selectBox.defaults, options);
            if ($.isArray(opts.values)){
                return _generateReplacer(opts.values);
            }
        }
        
        function getValue(){
            return _getValue(self.data('selectbox'));
        }
        
        function getIndex(){
            return _getIndex(self.data('selectbox'));
        }
        
        function setValue(value){
            var selectbox=self.data('selectbox');
            selectSpan=selectbox.selector.find('span[rel="'+value+'"]:first');
            if (selectSpan.length) {
                selectbox.replacer.find('span.cms-current-value').html(selectSpan.html()).attr('rel', value);
            };
        }
        
        function setEnabled(enable){
            var selectbox=self.data('selectbox');
            if (enable){
                selectbox.replacer.children().removeClass('ui-state-disabled');
                selectbox.replacer.unbind('click');
                selectbox.replacer.click(_start);
            }else{
                _close();
                selectbox.replacer.children().addClass('ui-state-disabled');
                selectbox.replacer.unbind('click');
            }
        }
	};
    
	$.fn.selectBox.defaults={
        width: null,
		open: null,
        select: null,
        appendTo: null
		
	}
})(jQuery);