package io.github.martinschneider.justtestlah.awsdevicefarm.devicefilter;

import com.amazonaws.services.devicefarm.model.DeviceAvailability;
import com.amazonaws.services.devicefarm.model.DeviceFilter;
import com.amazonaws.services.devicefarm.model.DeviceFilterAttribute;
import com.amazonaws.services.devicefarm.model.DeviceFilterOperator;

/** Pre-defined device filters for {@link DeviceAvailability}. */
public class DeviceFilterConstants {
  public static final DeviceFilter AVAILABLE_FILTER =
      new DeviceFilter()
          .withAttribute(DeviceFilterAttribute.AVAILABILITY)
          .withOperator(DeviceFilterOperator.EQUALS)
          .withValues(DeviceAvailability.AVAILABLE.toString());

  public static final DeviceFilter HIGHLY_AVAILABLE_FILTER =
      new DeviceFilter()
          .withAttribute(DeviceFilterAttribute.AVAILABILITY)
          .withOperator(DeviceFilterOperator.EQUALS)
          .withValues(DeviceAvailability.HIGHLY_AVAILABLE.toString());

  public static final DeviceFilter BUSY_FILTER =
      new DeviceFilter()
          .withAttribute(DeviceFilterAttribute.AVAILABILITY)
          .withOperator(DeviceFilterOperator.EQUALS)
          .withValues(DeviceAvailability.BUSY.toString());
}
