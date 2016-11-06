package org.jboss.seam.captcha;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.jboss.seam.core.Interpolator;

/**
 * Validates that the input entered by the user matches
 * the captcha image.
 * 
 * @author Gavin King
 * @author Marek Novotny
 *
 */
public class CaptchaResponseValidator implements ConstraintValidator<CaptchaResponse,String>
{

	private CaptchaResponse annotation = null;
	
	public CaptchaResponseValidator() {
		super();
	}

   public void initialize(CaptchaResponse constraintAnnotation)   {
	   annotation = constraintAnnotation; 
   }

   public boolean isValid(String value, ConstraintValidatorContext context)
   {
      boolean result = Captcha.instance().validateResponse(value);
      if (!result)
      {
    	  
         context.disableDefaultConstraintViolation();
         System.out.println("annotation.message=" + annotation.message());
         String template = Interpolator.instance().interpolate(annotation.message());
         context.buildConstraintViolationWithTemplate(template).addConstraintViolation();
      }
      return result;
   }

}
